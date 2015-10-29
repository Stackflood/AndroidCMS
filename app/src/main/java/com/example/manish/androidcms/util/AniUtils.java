package com.example.manish.androidcms.util;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

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
