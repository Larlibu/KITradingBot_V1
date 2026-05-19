# ADR-001: Drei Services statt Monolith

- Status: Accepted
- Datum: 2026-05-18
- Entscheider: Projektteam KITradingBot

## Kontext
Das Projekt kombiniert UI, Trading-Logik und ML-Training/Inferenz. Diese Teile haben unterschiedliche Laufzeit- und Technologieanforderungen (React/Node, Java/Spring, Python/ML-Stack).

## Entscheidung
Das System wird als drei lokale Services umgesetzt:
1. Frontend (React/Vite)
2. Backend (Spring Boot)
3. ML-Service (FastAPI)

## Konsequenzen
### Positiv
- Klare Verantwortlichkeiten pro Service.
- Python-ML-Stack kann unabhaengig vom Java-Backend entwickelt werden.
- Fehler in einem Teil koennen isolierter behandelt werden.

### Negativ
- Mehr Betriebsaufwand lokal (mehr Prozesse/Ports).
- API-Vertraege zwischen Services muessen sauber gepflegt werden.

## Alternativen
- Monolith in nur einer Sprache/framework.
- Frontend + Backend zusammen, ML als lokale Library im Backend.
