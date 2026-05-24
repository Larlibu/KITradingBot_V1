package com.kitradingbot.indicator;

import com.kitradingbot.model.Candle;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class IndicatorTest {

    private List<Candle> createMockCandles(int count) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            candles.add(new Candle(
                LocalDateTime.now().minusMinutes(count - i),
                100.0 + i,
                105.0 + i,
                95.0 + i,
                100.0 + i,
                1000.0
            ));
        }
        return candles;
    }

    @Test
    public void testSma() {
        SmaIndicator sma = new SmaIndicator();
        List<Candle> candles = createMockCandles(30);
        double result = sma.calculate(candles, 29, 20);
        assertTrue(result > 0);
    }

    @Test
    public void testRsi() {
        RsiIndicator rsi = new RsiIndicator();
        List<Candle> candles = createMockCandles(30);
        double result = rsi.calculate(candles, 29, 14);
        assertTrue(result >= 0 && result <= 100);
    }
}
