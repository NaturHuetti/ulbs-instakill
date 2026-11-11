# Lasttest-Ergebnisse PostgreSQL

## Gesamt (Aggregated)

| Nutzer | Requests | Fehler | Avg (ms) | Median (ms) | 95%ile (ms) | 99%ile (ms) | Max (ms) | RPS |
|---|---|---|---|---|---|---|---|---|
| 1 | 60 | 0 | 44.9 | 24 | 220 | 500 | 503 | 0.51 |
| 2 | 126 | 0 | 22.2 | 17 | 69 | 120 | 144 | 1.06 |
| 5 | 304 | 0 | 35.4 | 14 | 170 | 260 | 936 | 2.55 |
| 10 | 618 | 0 | 15.6 | 12 | 37 | 150 | 271 | 5.18 |

## CPU / Speicher

| Nutzer | Backend CPU Ø (%) | Backend CPU max (%) | Backend RSS max (MB) | Postgres CPU Ø (%) | Postgres CPU max (%) | Postgres RSS max (MB) |
|---|---|---|---|---|---|---|
| 1 | 2.0 | 32.0 | 238.0 | 0.3 | 16.2 | 55.0 |
| 2 | 1.8 | 15.5 | 162.0 | 0.5 | 16.6 | 67.0 |
| 5 | 2.8 | 24.0 | 235.0 | 0.6 | 13.5 | 86.0 |
| 10 | 3.5 | 55.6 | 202.0 | 1.4 | 17.7 | 178.0 |

CPU-Werte stammen aus `ps` (100 % = ein Kern), 1 Messwert/Sekunde.

## Pro Endpoint (Avg ms / Fehler / Requests)

| Endpoint | 1 Nutzer | 2 Nutzer | 5 Nutzer | 10 Nutzer |
|---|---|---|---|---|
| GET /api/analytics/me | 295 / 0 / 2 | 92 / 0 / 5 | 314 / 0 / 4 | 82 / 0 / 15 |
| GET /api/feed?sort=new | 28 / 0 / 24 | 19 / 0 / 37 | 37 / 0 / 104 | 13 / 0 / 207 |
| GET /api/feed?sort=top | 34 / 0 / 7 | 18 / 0 / 21 | 40 / 0 / 43 | 14 / 0 / 96 |
| GET /api/posts/{id} | 21 / 0 / 11 | 13 / 0 / 25 | 10 / 0 / 51 | 7 / 0 / 111 |
| GET /api/posts/{id}/comments | 24 / 0 / 4 | 17 / 0 / 9 | 20 / 0 / 28 | 9 / 0 / 72 |
| POST /api/auth/login | 292 / 0 / 1 | 124 / 0 / 2 | 195 / 0 / 5 | 158 / 0 / 10 |
| POST /api/posts | 84 / 0 / 3 | 12 / 0 / 5 | 19 / 0 / 9 | 10 / 0 / 11 |
| POST /api/posts/{id}/comments | 29 / 0 / 5 | 23 / 0 / 9 | 40 / 0 / 18 | 13 / 0 / 30 |
| POST /api/posts/{id}/likes/toggle | 56 / 0 / 3 | 21 / 0 / 13 | 25 / 0 / 42 | 13 / 0 / 66 |

## Vergleich mit SQLite (aus den Screenshots in ../locust, Aggregated)

| Nutzer | DB | Requests | Fehler | Avg (ms) | Median (ms) | 95%ile (ms) | Max (ms) |
|---|---|---|---|---|---|---|---|
| 1 | SQLite | 63 | 0 | 713.5 | 710 | 1400 | 2241 |
| 1 | PostgreSQL | 60 | 0 | 44.9 | 24 | 220 | 503 |
| 2 | SQLite | 127 | 0 | 658.5 | 640 | 1700 | 1997 |
| 2 | PostgreSQL | 126 | 0 | 22.2 | 17 | 69 | 144 |
| 5 | SQLite | 1120 | 16 | 1100.5 | 830 | 3400 | 10776 |
| 5 | PostgreSQL | 304 | 0 | 35.4 | 14 | 170 | 936 |
| 10 | PostgreSQL | 618 | 0 | 15.6 | 12 | 37 | 271 |

SQLite-Läufe hatten keine feste Dauer (manuell gestoppt), die Request-Zahlen sind daher nicht direkt vergleichbar, Durchschnitt, Median und Fehler schon. Für 10 Nutzer gibt es keinen SQLite-Lauf.
