# ADR-004: In-Memory Trade-Speicherung

- Status: Accepted
- Datum: 2026-05-18
- Entscheider: Projektteam KITradingBot

## Kontext
Das Projekt ist aktuell auf lokale Entwicklung, Analyse und Lernbetrieb ausgerichtet. Eine persistente Datenbank ist funktional nicht zwingend fuer den aktuellen Scope.

## Entscheidung
Trades werden im Backend in einem In-Memory-Repository gespeichert (`InMemoryTradeRepository`).

## Konsequenzen
### Positiv
- Sehr einfacher Start ohne DB-Setup.
- Schnelle Iteration fuer Strategieentwicklung.

### Negativ
- Kein Datenbestand ueber Neustarts.
- Keine Historisierung fuer laengere Auswertungen.
- Skalierung und Mehrbenutzerbetrieb nicht sinnvoll.

## Alternativen
- Relationale DB (z. B. PostgreSQL).
- Embedded DB (z. B. SQLite/H2) als Zwischenstufe.
