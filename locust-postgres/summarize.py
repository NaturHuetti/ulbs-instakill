"""Fasst alle '<N> user'-Ordner in summary.md zusammen."""
import csv
import re
from pathlib import Path

root = Path(__file__).resolve().parent
runs = sorted((d for d in root.glob("* user") if (d / "locust_stats.csv").exists()),
              key=lambda d: int(re.match(r"\d+", d.name).group()))


def rows(d):
    with open(d / "locust_stats.csv") as f:
        return list(csv.DictReader(f))


def res(d):
    p = d / "resources.csv"
    if not p.exists():
        return None
    with open(p) as f:
        r = []
        for line in list(f)[1:]:  # Zeile: t, cpu, rss, pcpu, prss (Dezimalkomma der Locale -> 7 Felder)
            v = line.strip().split(",")
            try:
                if len(v) == 7:
                    v = [v[0], f"{v[1]}.{v[2]}", v[3], f"{v[4]}.{v[5]}", v[6]]
                r.append(dict(zip(("t", "backend_cpu", "backend_rss_mb", "postgres_cpu", "postgres_rss_mb"),
                                  map(float, v))))
            except ValueError:
                pass
    avg = lambda k: sum(x[k] for x in r) / len(r)
    return (avg("backend_cpu"), max(x["backend_cpu"] for x in r), max(x["backend_rss_mb"] for x in r),
            avg("postgres_cpu"), max(x["postgres_cpu"] for x in r), max(x["postgres_rss_mb"] for x in r))


out = ["# Lasttest-Ergebnisse PostgreSQL", "",
       "## Gesamt (Aggregated)", "",
       "| Nutzer | Requests | Fehler | Avg (ms) | Median (ms) | 95%ile (ms) | 99%ile (ms) | Max (ms) | RPS |",
       "|---|---|---|---|---|---|---|---|---|"]
for d in runs:
    a = next(r for r in rows(d) if r["Name"] == "Aggregated")
    out.append(f"| {d.name.split()[0]} | {a['Request Count']} | {a['Failure Count']} | "
               f"{float(a['Average Response Time']):.1f} | {a['Median Response Time']} | {a['95%']} | {a['99%']} | "
               f"{float(a['Max Response Time']):.0f} | {float(a['Requests/s']):.2f} |")
out += ["", "## CPU / Speicher", "",
        "| Nutzer | Backend CPU Ø (%) | Backend CPU max (%) | Backend RSS max (MB) | Postgres CPU Ø (%) | Postgres CPU max (%) | Postgres RSS max (MB) |",
        "|---|---|---|---|---|---|---|"]
for d in runs:
    r = res(d)
    if r:
        out.append(f"| {d.name.split()[0]} | " + " | ".join(f"{v:.1f}" for v in r) + " |")
out += ["", "CPU-Werte stammen aus `ps` (100 % = ein Kern), 1 Messwert/Sekunde.", "",
        "## Pro Endpoint (Avg ms / Fehler / Requests)", ""]
names = [r["Name"] for r in rows(runs[0]) if r["Name"] != "Aggregated"] if runs else []
out += ["| Endpoint | " + " | ".join(f"{d.name.split()[0]} Nutzer" for d in runs) + " |",
        "|---|" + "---|" * len(runs)]
for n in names:
    cells = []
    for d in runs:
        m = next((r for r in rows(d) if r["Name"] == n), None)
        cells.append(f"{float(m['Average Response Time']):.0f} / {m['Failure Count']} / {m['Request Count']}" if m else "-")
    out.append(f"| {n} | " + " | ".join(cells) + " |")
out += ["", "## Vergleich mit SQLite (aus den Screenshots in ../locust, Aggregated)", "",
        "| Nutzer | DB | Requests | Fehler | Avg (ms) | Median (ms) | 95%ile (ms) | Max (ms) |",
        "|---|---|---|---|---|---|---|---|"]
sqlite = {1: (63, 0, 713.5, 710, 1400, 2241), 2: (127, 0, 658.5, 640, 1700, 1997),
          5: (1120, 16, 1100.5, 830, 3400, 10776)}
for d in runs:
    n = int(d.name.split()[0])
    a = next(r for r in rows(d) if r["Name"] == "Aggregated")
    if n in sqlite:
        q = sqlite[n]
        out.append(f"| {n} | SQLite | {q[0]} | {q[1]} | {q[2]} | {q[3]} | {q[4]} | {q[5]} |")
    out.append(f"| {n} | PostgreSQL | {a['Request Count']} | {a['Failure Count']} | "
               f"{float(a['Average Response Time']):.1f} | {a['Median Response Time']} | {a['95%']} | "
               f"{float(a['Max Response Time']):.0f} |")
out += ["", "SQLite-Läufe hatten keine feste Dauer (manuell gestoppt), die Request-Zahlen sind daher nicht direkt "
        "vergleichbar, Durchschnitt, Median und Fehler schon. Für 10 Nutzer gibt es keinen SQLite-Lauf."]
(root / "summary.md").write_text("\n".join(out) + "\n")
print("\n".join(out))
