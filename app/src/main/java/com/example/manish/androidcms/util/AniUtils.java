package com.example.manish.androidcms.util;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;

import com.example.manish.androidcms.R;

/**
 * Created by Manish on 10/15/2015.
 */
public class AniUtils {

    private AniUtils() {
        throw new AssertionError();
    }

    public static void startAnimation(View target, int aniResId) {
        startAnimation(target, aniResId, null);
    }

    public static void fadeIn(View target) {
        startAnimation(target, android.R.anim.fade_in, null);
        if (target.getVisibility() != View.VISIBLE)
            target.setVisibility(View.VISIBLE);
    }

    public static void flyIn(View target) {
        Context context = target.getContext();
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.reader_flyin);
        if (animation == null) {
            return;
        }

        // add small overshoot for bounce effect
        animation.setInterpolator(new OvershootInterpolator(0.9f));
        long duration = context.getResources().getInteger(android.R.integer.config_mediumAnimTime);
        animation.setDuration((long)(duration * 1.5f));

        target.startAnimation(animation);
        target.setVisibility(View.VISIBLE);
    }


    public static void startAnimation(View target, int aniResId, Animation.AnimationListener listener) {
        if (target == null) {
            return;
        }

        Animation animation = AnimationUtils.loadAnimation(target.getContext(), aniResId);
        if (animation == null) {
            return;
        }

        if (listener != null) {
            animation.setAnimationListener(listener);
        }

        target.startAnimation(animation);
    }
}
