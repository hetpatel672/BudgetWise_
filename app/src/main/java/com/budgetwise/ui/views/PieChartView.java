package com.budgetwise.ui.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PieChartView extends View {
    private Paint paint;
    private RectF rectF;
    private List<PieSlice> slices = new ArrayList<>();
    private boolean animationEnabled = false;
    private float animationProgress = 1.0f;
    
    private static final int[] COLORS = {
        Color.parseColor("#6750A4"),
        Color.parseColor("#625B71"),
        Color.parseColor("#7D5260"),
        Color.parseColor("#38A169"),
        Color.parseColor("#DD6B20"),
        Color.parseColor("#3182CE"),
        Color.parseColor("#E53E3E"),
        Color.parseColor("#805AD5")
    };

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectF = new RectF();
    }

    public void setData(Map<String, Double> data) {
        slices.clear();
        
        if (data == null || data.isEmpty()) {
            invalidate();
            return;
        }

        double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
        float startAngle = 0;
        int colorIndex = 0;

        for (Map.Entry<String, Double> entry : data.entrySet()) {
            float sweepAngle = (float) ((entry.getValue() / total) * 360);
            PieSlice slice = new PieSlice(
                entry.getKey(),
                entry.getValue(),
                startAngle,
                sweepAngle,
                COLORS[colorIndex % COLORS.length]
            );
            slices.add(slice);
            startAngle += sweepAngle;
            colorIndex++;
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

        if (slices.isEmpty()) {
            // Draw empty state
            paint.setColor(Color.GRAY);
            paint.setTextSize(48);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("No data", getWidth() / 2f, getHeight() / 2f, paint);
            return;
        }

        int size = Math.min(getWidth(), getHeight()) - 100;
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        
        rectF.set(centerX - size / 2f, centerY - size / 2f, 
                  centerX + size / 2f, centerY + size / 2f);

        for (PieSlice slice : slices) {
            paint.setColor(slice.color);
            float sweepAngle = slice.sweepAngle * animationProgress;
            canvas.drawArc(rectF, slice.startAngle, sweepAngle, true, paint);
        }
    }

    private static class PieSlice {
        String label;
        double value;
        float startAngle;
        float sweepAngle;
        int color;

        PieSlice(String label, double value, float startAngle, float sweepAngle, int color) {
            this.label = label;
            this.value = value;
            this.startAngle = startAngle;
            this.sweepAngle = sweepAngle;
            this.color = color;
        }
    }
}
