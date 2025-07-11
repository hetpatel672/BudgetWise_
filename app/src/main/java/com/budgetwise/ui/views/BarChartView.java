package com.budgetwise.ui.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BarChartView extends View {
    private Paint barPaint;
    private Paint textPaint;
    private List<BarData> bars = new ArrayList<>();
    private boolean animationEnabled = false;
    private float animationProgress = 1.0f;
    private double maxValue = 0;

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#6750A4"));
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(32);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(Map<String, Double> data) {
        bars.clear();
        
        if (data == null || data.isEmpty()) {
            invalidate();
            return;
        }

        maxValue = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            bars.add(new BarData(entry.getKey(), entry.getValue()));
        }

        if (animationEnabled) {
            animateChart();
        } else {
            invalidate();
        }
    }

    public void setAnimationEnabled(boolean enabled) {
        this.animationEnabled = enabled;
    }

    private void animateChart() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bars.isEmpty()) {
            textPaint.setTextSize(48);
            canvas.drawText("No data", getWidth() / 2f, getHeight() / 2f, textPaint);
            return;
        }

        int barWidth = getWidth() / bars.size() - 20;
        int maxBarHeight = getHeight() - 100;
        
        for (int i = 0; i < bars.size(); i++) {
            BarData bar = bars.get(i);
            
            float barHeight = maxValue > 0 ? 
                (float) ((bar.value / maxValue) * maxBarHeight * animationProgress) : 0;
            
            float left = i * (barWidth + 20) + 10;
            float top = getHeight() - barHeight - 50;
            float right = left + barWidth;
            float bottom = getHeight() - 50;
            
            canvas.drawRect(left, top, right, bottom, barPaint);
            
            // Draw label
            textPaint.setTextSize(24);
            canvas.drawText(bar.label, left + barWidth / 2f, getHeight() - 10, textPaint);
            
            // Draw value
            textPaint.setTextSize(20);
            canvas.drawText(String.format("$%.0f", bar.value), 
                left + barWidth / 2f, top - 10, textPaint);
        }
    }

    private static class BarData {
        String label;
        double value;

        BarData(String label, double value) {
            this.label = label;
            this.value = value;
        }
    }
}
