package com.budgetwise.ui.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

public class AnimationUtils {
    
    public static void fadeIn(View view) {
        fadeIn(view, 300, null);
    }
    
    public static void fadeIn(View view, long duration, Animator.AnimatorListener listener) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        fadeIn.setDuration(duration);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        
        if (listener != null) {
            fadeIn.addListener(listener);
        }
        
        fadeIn.start();
    }
    
    public static void fadeOut(View view) {
        fadeOut(view, 300, null);
    }
    
    public static void fadeOut(View view, long duration, Animator.AnimatorListener listener) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        fadeOut.setDuration(duration);
        fadeOut.setInterpolator(new AccelerateDecelerateInterpolator());
        
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
                if (listener != null) {
                    listener.onAnimationEnd(animation);
                }
            }
        });
        
        fadeOut.start();
    }
    
    public static void slideInFromRight(View view) {
        slideInFromRight(view, 400, null);
    }
    
    public static void slideInFromRight(View view, long duration, Animator.AnimatorListener listener) {
        view.setTranslationX(view.getWidth());
        view.setVisibility(View.VISIBLE);
        
        ObjectAnimator slideIn = ObjectAnimator.ofFloat(view, "translationX", view.getWidth(), 0f);
        slideIn.setDuration(duration);
        slideIn.setInterpolator(new FastOutSlowInInterpolator());
        
        if (listener != null) {
            slideIn.addListener(listener);
        }
        
        slideIn.start();
    }
    
    public static void slideInFromBottom(View view) {
        slideInFromBottom(view, 400, null);
    }
    
    public static void slideInFromBottom(View view, long duration, Animator.AnimatorListener listener) {
        view.setTranslationY(view.getHeight());
        view.setVisibility(View.VISIBLE);
        
        ObjectAnimator slideIn = ObjectAnimator.ofFloat(view, "translationY", view.getHeight(), 0f);
        slideIn.setDuration(duration);
        slideIn.setInterpolator(new FastOutSlowInInterpolator());
        
        if (listener != null) {
            slideIn.addListener(listener);
        }
        
        slideIn.start();
    }
    
    public static void scaleIn(View view) {
        scaleIn(view, 300, null);
    }
    
    public static void scaleIn(View view, long duration, Animator.AnimatorListener listener) {
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.setVisibility(View.VISIBLE);
        
        AnimatorSet scaleSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        
        scaleSet.playTogether(scaleX, scaleY, alpha);
        scaleSet.setDuration(duration);
        scaleSet.setInterpolator(new OvershootInterpolator());
        
        if (listener != null) {
            scaleSet.addListener(listener);
        }
        
        scaleSet.start();
    }
    
    public static void bounceIn(View view) {
        bounceIn(view, 600, null);
    }
    
    public static void bounceIn(View view, long duration, Animator.AnimatorListener listener) {
        view.setScaleX(0.3f);
        view.setScaleY(0.3f);
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        
        AnimatorSet bounceSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.3f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        
        bounceSet.playTogether(scaleX, scaleY, alpha);
        bounceSet.setDuration(duration);
        bounceSet.setInterpolator(new BounceInterpolator());
        
        if (listener != null) {
            bounceSet.addListener(listener);
        }
        
        bounceSet.start();
    }
    
    public static void pulse(View view) {
        pulse(view, 1000, 1.1f);
    }
    
    public static void pulse(View view, long duration, float scale) {
        AnimatorSet pulseSet = new AnimatorSet();
        ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(view, "scaleX", 1f, scale);
        ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(view, "scaleY", 1f, scale);
        ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(view, "scaleX", scale, 1f);
        ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(view, "scaleY", scale, 1f);
        
        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.playTogether(scaleXUp, scaleYUp);
        scaleUp.setDuration(duration / 2);
        
        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.playTogether(scaleXDown, scaleYDown);
        scaleDown.setDuration(duration / 2);
        
        pulseSet.playSequentially(scaleUp, scaleDown);
        pulseSet.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseSet.start();
    }
    
    public static void shake(View view) {
        shake(view, 500, 10f);
    }
    
    public static void shake(View view, long duration, float amplitude) {
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX", 
            0, amplitude, -amplitude, amplitude, -amplitude, amplitude/2, -amplitude/2, 0);
        shake.setDuration(duration);
        shake.setInterpolator(new AccelerateDecelerateInterpolator());
        shake.start();
    }
    
    public static void animateProgress(View progressView, int fromProgress, int toProgress) {
        animateProgress(progressView, fromProgress, toProgress, 1000);
    }
    
    public static void animateProgress(View progressView, int fromProgress, int toProgress, long duration) {
        ValueAnimator progressAnimator = ValueAnimator.ofInt(fromProgress, toProgress);
        progressAnimator.setDuration(duration);
        progressAnimator.setInterpolator(new FastOutSlowInInterpolator());
        
        progressAnimator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            if (progressView instanceof com.google.android.material.progressindicator.LinearProgressIndicator) {
                ((com.google.android.material.progressindicator.LinearProgressIndicator) progressView).setProgress(progress);
            }
        });
        
        progressAnimator.start();
    }
    
    public static void staggeredAnimation(ViewGroup parent, long delayBetweenItems) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(50f);
            
            child.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(i * delayBetweenItems)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
        }
    }
    
    public static void morphFab(View fab, View targetView) {
        // Simple morph animation for FAB
        AnimatorSet morphSet = new AnimatorSet();
        
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(fab, "scaleX", 1f, 0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(fab, "scaleY", 1f, 0f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(fab, "rotation", 0f, 180f);
        
        morphSet.playTogether(scaleX, scaleY, rotation);
        morphSet.setDuration(200);
        
        morphSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                fab.setVisibility(View.GONE);
                targetView.setVisibility(View.VISIBLE);
                scaleIn(targetView);
            }
        });
        
        morphSet.start();
    }
    
    public static void cardFlip(View frontView, View backView) {
        ObjectAnimator frontRotation = ObjectAnimator.ofFloat(frontView, "rotationY", 0f, 90f);
        frontRotation.setDuration(300);
        
        ObjectAnimator backRotation = ObjectAnimator.ofFloat(backView, "rotationY", -90f, 0f);
        backRotation.setDuration(300);
        
        frontRotation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                frontView.setVisibility(View.GONE);
                backView.setVisibility(View.VISIBLE);
                backRotation.start();
            }
        });
        
        frontRotation.start();
    }
    
    public static void countUpAnimation(android.widget.TextView textView, double fromValue, double toValue, String prefix, String suffix) {
        ValueAnimator countAnimator = ValueAnimator.ofFloat((float) fromValue, (float) toValue);
        countAnimator.setDuration(1000);
        countAnimator.setInterpolator(new FastOutSlowInInterpolator());
        
        countAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            textView.setText(String.format("%s%.2f%s", prefix, value, suffix));
        });
        
        countAnimator.start();
    }
}