"""
Modul für ein einfaches heuristisches Klassifikationsmodell.

Dieses Modell dient als leichter Fallback oder als Basislinie für das ML-Modell.
Es trifft Entscheidungen basierend auf festen technischen Regeln statt gelernten Mustern.
"""
from typing import List

class HeuristicClassifier:
    """
    Ein heuristischer Klassifikator mit einer Scikit-learn-kompatiblen Schnittstelle.
    Nutzt RSI und Momentum (Close vs SMA), um eine Kauf/Verkauf-Wahrscheinlichkeit zu schätzen.
    """

    def fit(self, x, y) -> "HeuristicClassifier":
        """
        Fit-Methode (Interface-Kompatibilität).
        Da die Regeln fest sind, findet kein Training statt.
        """
        return self

    def predict(self, x: List[List[float]]) -> List[int]:
        """
        Sagt die Klasse voraus (1 für STEIGT, 0 für FÄLLT).
        Schwellenwert ist 0.5.
        """
        probs = self.predict_proba(x)
        return [1 if row[1] >= 0.5 else 0 for row in probs]

    def predict_proba(self, x: List[List[float]]) -> List[List[float]]:
        """
        Berechnet Wahrscheinlichkeiten basierend auf einer gewichteten Heuristik.
        
        Logik:
        1. Momentum Score: Wie weit ist der Preis vom SMA entfernt? (Gedeckelt auf +/- 250 Einheiten)
        2. RSI Score: Ist der RSI über- oder unterverkauft? (Skaliert auf +/- 1.0)
        
        Kombination: 65% Momentum + 35% RSI
        
        Args:
            x: Liste von Feature-Vektoren. 
               Erwartet an Index 2 den RSI und an Index 3 die Diff (Close - SMA).
        
        Returns:
            Liste von Wahrscheinlichkeitspaaren [P(FÄLLT), P(STEIGT)].
        """
        probabilities: List[List[float]] = []
        
        for row in x:
            # Relevante Features extrahieren
            # Index 2 = RSI (0-100)
            # Index 3 = close_minus_sma (Differenz in USDT)
            rsi = float(row[2])
            close_minus_sma = float(row[3])

            # --- 1. Momentum Berechnung ---
            # Wir normalisieren den Abstand zum SMA auf einen Bereich von -1 bis 1.
            # Ein Abstand von 250 USDT wird hier als Maximum für die Heuristik angenommen.
            momentum_score = max(-1.0, min(1.0, close_minus_sma / 250.0))

            # --- 2. RSI Berechnung ---
            # Wir zentrieren den RSI um 50 (Neutral) und skalieren auf -1 bis 1.
            # RSI 100 -> 1.0, RSI 0 -> -1.0, RSI 50 -> 0.0
            rsi_score = max(-1.0, min(1.0, (rsi - 50.0) / 50.0))

            # --- 3. Score Kombination ---
            # Trendfolge (Momentum) wird höher gewichtet als der Oszillator (RSI).
            up_score = 0.65 * momentum_score + 0.35 * rsi_score

            # Umwandlung des Scores (-1 bis 1) in eine Wahrscheinlichkeit (0 bis 1).
            # Ein Score von 0 ergibt 0.5 Wahrscheinlichkeit.
            up_prob = max(0.01, min(0.99, (up_score + 1.0) / 2.0))

            # Ergebnis-Paar für Scikit-Learn Kompatibilität [Down, Up]
            probabilities.append([1.0 - up_prob, up_prob])
            
        return probabilities
