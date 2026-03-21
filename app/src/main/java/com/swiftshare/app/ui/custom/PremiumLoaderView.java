package com.swiftshare.app.ui.custom;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.swiftshare.app.R;

public class PremiumLoaderView extends View {

    private Paint ringPaint;
    private Paint pulsePaint;
    private RectF bounds;
    
    private float rotationAngle = 0f;
    private float pulseScale = 1f;
    private float pulseAlpha = 0.3f;
    
    private float strokeWidth;
    private SweepGradient sweepGradient;
    private Matrix gradientMatrix;

    private ValueAnimator rotationAnimator;
    private ValueAnimator pulseAnimator;
    private AnimatorSet animatorSet;
    
    private int startColor;
    private int centerColor;
    private int endColor;

    public PremiumLoaderView(Context context) {
        super(context);
        init(context);
    }

    public PremiumLoaderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PremiumLoaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        strokeWidth = context.getResources().getDisplayMetrics().density * 5f; // 5dp

        startColor = ContextCompat.getColor(context, R.color.gradient_start);
        centerColor = ContextCompat.getColor(context, R.color.gradient_center);
        endColor = ContextCompat.getColor(context, R.color.gradient_end);

        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(strokeWidth);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);

        pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pulsePaint.setStyle(Paint.Style.FILL);

        bounds = new RectF();
        gradientMatrix = new Matrix();

        setupAnimators();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = strokeWidth / 2f + 4f;
        bounds.set(padding, padding, w - padding, h - padding);

        int[] colors = {Color.TRANSPARENT, startColor, centerColor, endColor, startColor};
        float[] positions = {0f, 0.25f, 0.5f, 0.75f, 1f};
        
        sweepGradient = new SweepGradient(w / 2f, h / 2f, colors, positions);
        ringPaint.setShader(sweepGradient);
    }

    private void setupAnimators() {
        // Smooth continuous rotation
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f);
        rotationAnimator.setDuration(1200);
        rotationAnimator.setInterpolator(new LinearInterpolator());
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.addUpdateListener(animation -> {
            rotationAngle = (float) animation.getAnimatedValue();
            invalidate();
        });

        // Sophisticated pulse effect for the inner glow
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            pulseScale = 0.7f + (0.3f * fraction); // Scale between 0.7 and 1.0
            pulseAlpha = 0.1f + (0.2f * (1f - fraction)); // Alpha between 0.1 and 0.3
            invalidate();
        });

        animatorSet = new AnimatorSet();
        animatorSet.playTogether(rotationAnimator, pulseAnimator);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        // Draw Pulsing Inner Glow
        pulsePaint.setColor(centerColor);
        pulsePaint.setAlpha((int) (255 * pulseAlpha));
        float radius = (bounds.width() / 2f) * pulseScale;
        canvas.drawCircle(cx, cy, radius, pulsePaint);

        // Draw Rotating Gradient Ring
        if (sweepGradient != null) {
            gradientMatrix.setRotate(rotationAngle, cx, cy);
            sweepGradient.setLocalMatrix(gradientMatrix);
            canvas.drawArc(bounds, 0f, 360f, false, ringPaint);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animatorSet != null && !animatorSet.isStarted()) {
            animatorSet.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animatorSet != null) {
            animatorSet.cancel();
        }
    }

    public void startAnimation() {
        if (animatorSet != null && !animatorSet.isRunning()) {
            animatorSet.start();
        }
    }

    public void stopAnimation() {
        if (animatorSet != null) {
            animatorSet.cancel();
        }
    }
}
