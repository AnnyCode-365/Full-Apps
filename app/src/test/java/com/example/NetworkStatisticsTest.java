package com.example;

import com.example.jitter.NetworkStatistics;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Testes unitários para o cálculo de Jitter e Estatísticas de Latência.
 */
public class NetworkStatisticsTest {

    @Test
    public void testJitterAndLatencyCalculations() {
        NetworkStatistics stats = new NetworkStatistics();
        stats.addSample(30, true);
        stats.addSample(40, true);
        stats.addSample(35, true);

        assertEquals(30, stats.getMinLatency());
        assertEquals(40, stats.getMaxLatency());
        assertEquals(35, stats.getAvgLatency());

        // Diferenças: |40 - 30| = 10, |35 - 40| = 5 -> Média = (10 + 5) / 2 = 7.5
        assertEquals(7.5, stats.getJitter(), 0.01);
        assertEquals(0.0, stats.getPacketLoss(), 0.01);
    }

    @Test
    public void testPacketLossCalculation() {
        NetworkStatistics stats = new NetworkStatistics();
        stats.addSample(30, true);
        stats.addSample(0, false); // perdido
        stats.addSample(30, true);
        stats.addSample(0, false); // perdido

        assertEquals(50.0, stats.getPacketLoss(), 0.01);
    }
}
