/**
 * React-Komponente zur Anzeige einer Tabelle mit ausgeführten Trades.
 * Wird verwendet, um sowohl Live-Transaktionen als auch Backtest-Ergebnisse darzustellen.
 * @param {object} props - Die Props der Komponente.
 * @param {Array<object>} props.trades - Eine Liste von Trade-Objekten, die angezeigt werden sollen.
 *                                      Jedes Trade-Objekt sollte Felder wie id, timestamp, side, price, quantity, pnl, capitalAfterTrade enthalten.
 * @returns {JSX.Element} Die gerenderte Tabelle der Trades.
 */
export default function TradesTable({ trades }) {
  return (
    <section className="panel trades-list">
      <h2>Ausgeführte Trades (Simulation)</h2>
      <div className="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Zeitpunkt</th>
              <th>Seite</th>
              <th>Preis</th>
              <th>Menge</th>
              <th title="Gewinn und Verlust">GuV</th>
              <th>Kapital</th>
            </tr>
          </thead>
          <tbody>
            {/* Rendert jede Zeile der Tabelle basierend auf den übergebenen Trades */}
            {trades.map((t) => (
              <tr key={t.id}>
                <td>{t.id}</td>
                <td>{new Date(t.timestamp).toLocaleString()}</td> {/* Formatiert den Zeitstempel */}
                <td className={t.side === "BUY" ? "side-buy" : "side-sell"}>
                  {t.side === "BUY" ? "KAUF" : "VERKAUF"}
                </td>
                <td>${t.price.toFixed(2)}</td> {/* Preis auf 2 Nachkommastellen */}
                <td>{t.quantity.toFixed(5)}</td> {/* Menge auf 5 Nachkommastellen */}
                <td style={{ color: t.pnl >= 0 ? "#4CAF50" : "#F44336" }}> {/* Farbe basierend auf PnL */}
                  {t.pnl !== 0 ? `$${t.pnl.toFixed(2)}` : "-"} {/* Zeigt PnL nur an, wenn nicht 0 */}
                </td>
                <td>${t.capitalAfterTrade.toFixed(2)}</td> {/* Kapital nach dem Trade */}
              </tr>
            ))}
            {/* Zeigt eine Meldung an, wenn keine Trades vorhanden sind */}
            {trades.length === 0 && (
              <tr>
                <td colSpan="7" style={{ textAlign: "center", padding: "20px" }}>
                  Noch keine Trades ausgeführt.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
