package com.example.jitter;

/**
 * Analisador de qualidade de conexão, cálculo de instabilidade e variação de latência.
 * Compatível com Java 7.
 */
public class LatencyAnalyzer {

    public enum QualityGrade {
        EXCELENTE,
        BOA,
        MODERADA,
        INSTAVEL
    }

    public static QualityGrade evaluateQuality(long avgLatency, double jitter, double packetLoss) {
        if (packetLoss > 10.0 || jitter > 30.0 || avgLatency > 250) {
            return QualityGrade.INSTAVEL;
        } else if (packetLoss > 3.0 || jitter > 15.0 || avgLatency > 120) {
            return QualityGrade.MODERADA;
        } else if (jitter > 8.0 || avgLatency > 70) {
            return QualityGrade.BOA;
        } else {
            return QualityGrade.EXCELENTE;
        }
    }

    public static boolean isOscillationDetected(double jitter, double packetLoss, double variance) {
        return jitter >= 20.0 || packetLoss >= 5.0 || variance >= 400.0;
    }

    public static String getComparisonSummary(long baseline, long currentWithVpn) {
        if (baseline <= 0 || currentWithVpn <= 0) {
            return "Sem dados comparativos suficientes";
        }
        long diff = currentWithVpn - baseline;
        if (diff > 0) {
            return "Variação com VPN: +" + diff + " ms (sobrecarga do túnel)";
        } else if (diff < 0) {
            return "Variação com VPN: " + diff + " ms (rota otimizada)";
        } else {
            return "Latência idêntica antes e depois da VPN";
        }
    }
}
