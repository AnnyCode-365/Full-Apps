package com.example.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * Gráfico vetorial em tempo real para visualização de tráfego de rede e latência.
 * Renderiza forma de onda com gradiente neon sobre grade de coordenadas.
 * Compatível com Java 7.
 */
public class NetworkGraph extends View {

    public enum Mode {
        BANDWIDTH,
        LATENCY
    }

    private Mode mode = Mode.BANDWIDTH;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line1Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill1Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line2Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill2Paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path path1 = new Path();
    private final Path fillPath1 = new Path();
    private final Path path2 = new Path();
    private final Path fillPath2 = new Path();

    private List<Float> dataSeries1 = new ArrayList<Float>();
    private List<Float> dataSeries2 = new ArrayList<Float>();

    public NetworkGraph(Context context) {
        super(context);
        init();
    }

    public NetworkGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NetworkGraph(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint.setColor(Color.parseColor("#1F293D"));
        gridPaint.setStrokeWidth(1.5f);
        gridPaint.setStyle(Paint.Style.STROKE);

        textPaint.setColor(Color.parseColor("#94A3B8"));
        textPaint.setTextSize(26f);

        // Download / Latência: Ciano Neon
        line1Paint.setColor(Color.parseColor("#00E5FF"));
        line1Paint.setStrokeWidth(3.5f);
        line1Paint.setStyle(Paint.Style.STROKE);
        line1Paint.setStrokeCap(Paint.Cap.ROUND);
        line1Paint.setStrokeJoin(Paint.Join.ROUND);

        // Upload: Roxo Neon
        line2Paint.setColor(Color.parseColor("#A855F7"));
        line2Paint.setStrokeWidth(3.5f);
        line2Paint.setStyle(Paint.Style.STROKE);
        line2Paint.setStrokeCap(Paint.Cap.ROUND);
        line2Paint.setStrokeJoin(Paint.Join.ROUND);

        fill1Paint.setStyle(Paint.Style.FILL);
        fill2Paint.setStyle(Paint.Style.FILL);
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        invalidate();
    }

    public synchronized void setData(List<Float> series1, List<Float> series2) {
        this.dataSeries1 = (series1 != null) ? new ArrayList<Float>(series1) : new ArrayList<Float>();
        this.dataSeries2 = (series2 != null) ? new ArrayList<Float>(series2) : new ArrayList<Float>();
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        int padLeft = 70;
        int padBottom = 40;
        int padTop = 20;
        int padRight = 20;

        int plotW = w - padLeft - padRight;
        int plotH = h - padTop - padBottom;
        if (plotW <= 0 || plotH <= 0) return;

        // Desenhar linhas de grade horizontais
        int gridLines = 4;
        for (int i = 0; i <= gridLines; i++) {
            float y = padTop + (plotH * (float) i / gridLines);
            canvas.drawLine(padLeft, y, w - padRight, y, gridPaint);
        }

        // Determinar valor máximo de escala
        float maxVal = 10.0f;
        synchronized (this) {
            for (Float val : dataSeries1) {
                if (val != null && val > maxVal) maxVal = val;
            }
            if (mode == Mode.BANDWIDTH) {
                for (Float val : dataSeries2) {
                    if (val != null && val > maxVal) maxVal = val;
                }
            }
        }
        maxVal = maxVal * 1.2f; // margem superior

        // Rótulos do eixo Y
        canvas.drawText(String.format("%.0f", maxVal), 10, padTop + 24, textPaint);
        canvas.drawText(String.format("%.0f", maxVal / 2), 10, padTop + (plotH / 2f) + 10, textPaint);
        canvas.drawText("0", 30, h - padBottom, textPaint);

        // Desenhar Série 1
        synchronized (this) {
            drawSeries(canvas, dataSeries1, maxVal, padLeft, padTop, plotW, plotH, line1Paint, fill1Paint,
                    Color.parseColor("#4D00E5FF"), Color.parseColor("#0000E5FF"), path1, fillPath1);

            // Desenhar Série 2 se for Bandwidth
            if (mode == Mode.BANDWIDTH && !dataSeries2.isEmpty()) {
                drawSeries(canvas, dataSeries2, maxVal, padLeft, padTop, plotW, plotH, line2Paint, fill2Paint,
                        Color.parseColor("#4DA855F7"), Color.parseColor("#00A855F7"), path2, fillPath2);
            }
        }
    }

    private void drawSeries(Canvas canvas, List<Float> data, float maxVal, int padLeft, int padTop,
                            int plotW, int plotH, Paint linePaint, Paint fillPaint,
                            int colorTop, int colorBottom, Path lineP, Path fillP) {
        if (data.size() < 2) return;

        lineP.reset();
        fillP.reset();

        float stepX = (float) plotW / (data.size() - 1);
        float baseY = padTop + plotH;

        for (int i = 0; i < data.size(); i++) {
            float val = data.get(i) != null ? data.get(i) : 0f;
            float x = padLeft + (i * stepX);
            float ratio = Math.min(1.0f, Math.max(0.0f, val / maxVal));
            float y = baseY - (ratio * plotH);

            if (i == 0) {
                lineP.moveTo(x, y);
                fillP.moveTo(x, baseY);
                fillP.lineTo(x, y);
            } else {
                lineP.lineTo(x, y);
                fillP.lineTo(x, y);
            }
        }

        float lastX = padLeft + ((data.size() - 1) * stepX);
        fillP.lineTo(lastX, baseY);
        fillP.close();

        fillPaint.setShader(new LinearGradient(0, padTop, 0, baseY, colorTop, colorBottom, Shader.TileMode.CLAMP));
        canvas.drawPath(fillP, fillPaint);
        canvas.drawPath(lineP, linePaint);
    }
}
