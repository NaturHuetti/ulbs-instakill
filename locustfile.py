"""Locust-Lasttest fuer das Instakill-Backend (SQLite).

Start:  locust -f locustfile.py --host http://localhost:8080
UI:     http://localhost:8089

Voraussetzung: Backend laeuft (./gradlew bootRun) und die DB wurde ggf. mit
scripts/populate_db.py befuellt. Login erfolgt als `charlie` / `password`
(vom populate-Skript angelegt); der JWT-Cookie wird von der Locust-Session
automatisch mitgesendet.
"""
import random

from locust import HttpUser, between, task

USERNAME = "charlie"
PASSWORD = "password"


class InstakillUser(HttpUser):
    wait_time = between(1, 3)

    def on_start(self):
        self.post_ids = []
        with self.client.post(
            "/api/auth/login",
            json={"usernameOrEmail": USERNAME, "password": PASSWORD},
            name="POST /api/auth/login",
            catch_response=True,
        ) as r:
            if r.status_code != 200:
                r.failure(f"Login fehlgeschlagen: {r.status_code}")
        self._load_feed("new")

    def _load_feed(self, sort, page=0):
        r = self.client.get(
            "/api/feed",
            params={"sort": sort, "page": page, "size": 10},
            name=f"GET /api/feed?sort={sort}",
        )
        if r.status_code == 200:
            ids = [p["id"] for p in r.json() if "id" in p]
            if ids:
                self.post_ids = ids

    def _random_post(self):
        return random.choice(self.post_ids) if self.post_ids else None

    @task(10)
    def feed_new(self):
        self._load_feed("new", random.randint(0, 5))

    @task(5)
    def feed_top(self):
        self._load_feed("top", random.randint(0, 5))

    @task(5)
    def view_post(self):
        if pid := self._random_post():
            self.client.get(f"/api/posts/{pid}", name="GET /api/posts/{id}")

    @task(3)
    def list_comments(self):
        if pid := self._random_post():
            self.client.get(f"/api/posts/{pid}/comments", name="GET /api/posts/{id}/comments")

    @task(3)
    def toggle_like(self):
        if pid := self._random_post():
            self.client.post(f"/api/posts/{pid}/likes/toggle", name="POST /api/posts/{id}/likes/toggle")

    @task(2)
    def add_comment(self):
        if pid := self._random_post():
            self.client.post(
                f"/api/posts/{pid}/comments",
                json={"content": "Lasttest-Kommentar"},
                name="POST /api/posts/{id}/comments",
            )

    @task(1)
    def create_post(self):
        self.client.post("/api/posts", json={"content": "Lasttest-Post"}, name="POST /api/posts")

    @task(1)
    def analytics(self):
        self.client.get("/api/analytics/me", name="GET /api/analytics/me")
