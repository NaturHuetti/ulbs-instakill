#!/usr/bin/env python3
"""Fill the instakill PostgreSQL database with configurable fake data.

Setup:   pip install faker bcrypt "psycopg[binary]"
Usage:   python scripts/populate_db.py                      # defaults: 10,000 users
         python scripts/populate_db.py --users 500 --seed 1
         python scripts/populate_db.py --dsn postgresql://user:pw@host:5432/instakill

The connection string comes from --dsn, else $DATABASE_URL, else the local default
(instakill / instakill_password @ localhost:5432/instakill).

Existing comments, likes and posts are deleted, as are all users except `alice`
and `bob`. User `charlie` (password: "password") is (re)created. Alice, bob,
charlie and the generated users all receive posts.

The backend must have been started once so Hibernate has created the schema. It may keep
running while the script runs (PostgreSQL has no global write lock).
"""
import argparse
import os
import random
import sys
import uuid
from datetime import datetime, time, timedelta, timezone

import bcrypt
import psycopg
from faker import Faker

DEFAULT_DSN = "postgresql://instakill:instakill_password@localhost:5432/instakill"
KEEP_USERS = ("alice", "bob")
PHOTO_URL = "https://picsum.photos/seed/{seed}/800/600"
VIDEO_URLS = (
    "https://www.w3schools.com/html/mov_bbb.mp4",
    "https://www.w3schools.com/html/movie.mp4",
)


def parse_args():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--dsn", default=os.environ.get("DATABASE_URL", DEFAULT_DSN), help="PostgreSQL connection string")
    p.add_argument("--users", type=int, default=10_000, help="total users incl. alice, bob, charlie (default 10000)")
    p.add_argument("--posts-min", type=int, default=10)
    p.add_argument("--posts-max", type=int, default=20)
    p.add_argument("--comments-max", type=int, default=10, help="max comments per post (min 0)")
    p.add_argument("--likes-max", type=int, default=20, help="max likes per post (min 0)")
    p.add_argument("--max-words", type=int, default=10, help="max words per post")
    p.add_argument("--batch-users", type=int, default=250, help="users per commit batch")
    p.add_argument("--password", default="password", help="password for charlie and generated users")
    p.add_argument("--seed", type=int, default=None, help="seed for reproducible data")
    return p.parse_args()


def ms(dt: datetime) -> int:
    return int(dt.timestamp() * 1000)


def from_ms(value: int) -> datetime:
    return datetime.fromtimestamp(value / 1000, tz=timezone.utc)


def random_time_between(fake: Faker, start: datetime, end: datetime) -> datetime:
    """Random moment in [start, end]; the day comes from faker.date_between_dates."""
    day = fake.date_between_dates(date_start=start.date(), date_end=end.date())
    t = datetime.combine(day, time(random.randint(0, 23), random.randint(0, 59), random.randint(0, 59)),
                         tzinfo=timezone.utc)
    return min(max(t, start), end)


def rand_ms(start_ms: int, end_ms: int) -> int:
    """Fast random timestamp for comments/likes (faker is too slow for millions of rows)."""
    return random.randint(start_ms, end_ms)


def flush(cur, table, columns, rows):
    """Bulk-load rows with COPY (much faster than INSERT for millions of rows)."""
    if rows:
        with cur.copy(f"COPY {table} ({columns}) FROM STDIN") as copy:
            for row in rows:
                copy.write_row(row)
        rows.clear()


def main():
    a = parse_args()
    if a.users < 3:
        sys.exit("--users must be at least 3 (alice, bob, charlie)")
    if a.seed is not None:
        random.seed(a.seed)
        Faker.seed(a.seed)
    fake = Faker()

    now = datetime.now(timezone.utc)
    week_ago = now - timedelta(days=7)

    # One bcrypt hash is reused (hashing 10k users at cost 12 would take ~40 minutes).
    pw_hash = bcrypt.hashpw(a.password.encode(), bcrypt.gensalt(rounds=10)).decode()

    try:
        conn = psycopg.connect(a.dsn, autocommit=True)  # explicit transaction control
    except psycopg.OperationalError as e:
        sys.exit(f"Cannot connect to PostgreSQL ({e}). Is it running and is --dsn correct?")
    cur = conn.cursor()

    # --- clean up + users (one transaction) -----------------------------------------------
    with conn.transaction():
        for table in ("comments", "likes", "posts"):
            cur.execute(f"DELETE FROM {table}")
        cur.execute("DELETE FROM users WHERE username <> ALL(%s)", (list(KEEP_USERS),))
        cur.execute("SELECT username, id FROM users")
        kept = {name: str(uid).strip() for name, uid in cur.fetchall()}

    user_ids = list(kept.values())
    taken = {"alice", "bob", "charlie"} | set(kept)
    new_users = []

    def add_user(username, email):
        uid = str(uuid.uuid4())
        created = now - timedelta(days=random.randint(8, 60))
        new_users.append((uid, created, email, pw_hash, username))
        user_ids.append(uid)

    add_user("charlie", "charlie@example.com")
    while len(user_ids) < a.users:
        name = fake.user_name().lower()
        if name in taken:
            name = f"{name}{len(taken)}"  # guarantees uniqueness
        taken.add(name)
        add_user(name, f"{name}@example.com")
    n_new = len(new_users)
    with conn.transaction():
        flush(cur, "users", "id, created_at, email, password_hash, username", new_users)
    print(f"Users: {len(user_ids)} ({n_new} created, kept {', '.join(kept)})")

    # --- posts / comments / likes (one transaction per batch of users) --------------------
    comment_pool = [fake.sentence(nb_words=random.randint(3, 15))[:500] for _ in range(2000)]
    post_cols = "id, author_id, comment_count, content, created_at, image_url, like_count, updated_at"
    comment_cols = "id, author_id, content, created_at, post_id"
    like_cols = "id, created_at, post_id, user_id"
    now_ms = ms(now)
    posts, comments, likes = [], [], []
    totals = [0, 0, 0]

    def commit_batch():
        with conn.transaction():
            flush(cur, "posts", post_cols, posts)
            flush(cur, "comments", comment_cols, comments)
            flush(cur, "likes", like_cols, likes)

    for i, author in enumerate(user_ids, 1):
        for _ in range(random.randint(a.posts_min, a.posts_max)):
            pid = str(uuid.uuid4())
            created = random_time_between(fake, week_ago, now)
            created_ms = ms(created)
            if random.random() < 0.5:
                url = PHOTO_URL.format(seed=random.randint(1, 1_000_000))
            else:
                url = random.choice(VIDEO_URLS)
            content = " ".join(fake.words(nb=random.randint(1, a.max_words)))

            n_comments = random.randint(0, a.comments_max)
            for _ in range(n_comments):
                comments.append((str(uuid.uuid4()), random.choice(user_ids), random.choice(comment_pool),
                                 from_ms(rand_ms(created_ms, now_ms)), pid))
            # distinct users per post -> no duplicate (post, user) likes
            n_likes = min(random.randint(0, a.likes_max), len(user_ids))
            for uid in random.sample(user_ids, n_likes):
                likes.append((str(uuid.uuid4()), from_ms(rand_ms(created_ms, now_ms)), pid, uid))

            posts.append((pid, author, n_comments, content, created, url, n_likes, created))
            totals[0] += 1
            totals[1] += n_comments
            totals[2] += n_likes

        if i % a.batch_users == 0 or i == len(user_ids):
            commit_batch()
            print(f"  {i}/{len(user_ids)} users done", end="\r", flush=True)
    print()

    verify(cur)
    print(f"Done: {totals[0]} posts, {totals[1]} comments, {totals[2]} likes")
    conn.close()


def verify(cur):
    checks = {
        "posts without author": "SELECT COUNT(*) FROM posts WHERE author_id NOT IN (SELECT id FROM users)",
        "comments without post": "SELECT COUNT(*) FROM comments WHERE post_id NOT IN (SELECT id FROM posts)",
        "comments without author": "SELECT COUNT(*) FROM comments WHERE author_id NOT IN (SELECT id FROM users)",
        "likes without post": "SELECT COUNT(*) FROM likes WHERE post_id NOT IN (SELECT id FROM posts)",
        "likes without user": "SELECT COUNT(*) FROM likes WHERE user_id NOT IN (SELECT id FROM users)",
        "duplicate likes": "SELECT COUNT(*) FROM (SELECT 1 FROM likes GROUP BY post_id, user_id HAVING COUNT(*) > 1) t",
        "wrong counters": """SELECT COUNT(*) FROM posts p WHERE
            like_count != (SELECT COUNT(*) FROM likes WHERE post_id = p.id) OR
            comment_count != (SELECT COUNT(*) FROM comments WHERE post_id = p.id)""",
    }
    bad = {}
    for k, q in checks.items():
        cur.execute(q)
        bad[k] = cur.fetchone()[0]
    bad = {k: v for k, v in bad.items() if v}
    if bad:
        sys.exit(f"Integrity check failed: {bad}")
    print("Integrity check passed")


if __name__ == "__main__":
    main()
