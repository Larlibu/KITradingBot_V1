"""
Modul für ein einfaches heuristisches Klassifikationsmodell.

Dieses Modell dient als leichter Fallback oder als Basislinie für das ML-Modell.
Es implementiert eine Scikit-learn-ähnliche Schnittstelle (`fit`, `predict`, `predict_proba`),
um austauschbar mit echten ML-Modellen verwendet werden zu können.
"""

class HeuristicClassifier:
    """
    Ein leichter heuristischer Klassifikator mit einer Scikit-learn-ähnlichen Schnittstelle.
    Das Modell trifft Vorhersagen basierend auf vordefinierten Regeln, die technische Indikatoren nutzen.

    Die erwartete Reihenfolge der Features im Eingabevektor `x` ist:
    [0: close, 1: sma, 2: rsi, 3: close_minus_sma]
    """

    def fit(self, x, y) -> "HeuristicClassifier":
        """
        Die Fit-Methode für den HeuristicClassifier.
        Da dieses Modell keine Parameter lernt, tut diese Methode nichts und gibt sich selbst zurück.

        Args:
            x: Trainingsdaten (Features).
            y: Zielvariablen (Labels).

        Returns:
            HeuristicClassifier: Die Instanz des Klassifikators.
        """
        return self

    def predict(self, x: List[List[float]]) -> List[int]:
        """
        Führt Vorhersagen für eine Liste von Feature-Vektoren durch.
        Gibt 1 für 'STEIGT' und 0 für 'FÄLLT' zurück, basierend auf einer 50%-Wahrscheinlichkeitsschwelle.

        Args:
            x (List[List[float]]): Eine Liste von Feature-Vektoren.

        Returns:
            List[int]: Eine Liste der vorhergesagten Klassen (0 oder 1).
        """
        # Ruft die Wahrscheinlichkeiten ab und konvertiert sie in Klassen-Labels
        probs = self.predict_proba(x)
        return [1 if row[1] >= 0.5 else 0 for row in probs]

    def predict_proba(self, x: List[List[float]]) -> List[List[float]]:
        """
        Berechnet die Wahrscheinlichkeiten für jede Klasse (0: FÄLLT, 1: STEIGT)
        basierend auf heuristischen Regeln.

        Die Heuristik kombiniert den Momentum-Score (aus `close_minus_sma`)
        und den RSI-Score.

        Args:
            x (List[List[float]]): Eine Liste von Feature-Vektoren.

        Returns:
            List[List[float]]: Eine Liste von Wahrscheinlichkeits-Paaren [P(FÄLLT), P(STEIGT)].
        """
        probabilities: List[List[float]] = []
        for row in x:
            # Extrahiert die relevanten Features
            # close = float(row[0]) # Nicht direkt für die Heuristik verwendet
            # sma = float(row[1])   # Nicht direkt für die Heuristik verwendet
            rsi = float(row[2])
            close_minus_sma = float(row[3])

            # Berechnet einen Momentum-Score basierend auf der Differenz zwischen Close und SMA.
            # Normalisiert den Wert zwischen -1.0 und 1.0.
            momentum_score = max(-1.0, min(1.0, close_minus_sma / 250.0))

            # Berechnet einen RSI-Score, der anzeigt, ob der RSI überkauft/überverkauft ist.
            # Normalisiert den Wert zwischen -1.0 und 1.0 (RSI 50 = 0, RSI 100 = 1, RSI 0 = -1).
            rsi_score = max(-1.0, min(1.0, (rsi - 50.0) / 50.0))

            # Kombiniert die Scores heuristisch. Momentum wird stärker gewichtet.
            up_score = 0.65 * momentum_score + 0.35 * rsi_score

            # Konvertiert den kombinierten Score in eine Wahrscheinlichkeit zwischen 0.01 und 0.99.
            # Ein Score von -1.0 ergibt 0.01, ein Score von 1.0 ergibt 0.99.
            up_prob = max(0.01, min(0.99, (up_score + 1.0) / 2.0))

            # Die Wahrscheinlichkeit für 'FÄLLT' ist 1 - P('STEIGT').
            probabilities.append([1.0 - up_prob, up_prob])
        return probabilities
