package com.example.manish.androidcms.widgets;

import android.content.Context;
import android.util.AttributeSet;

import org.wordpress.android.util.widgets.AutoResizeTextView;

/**
 * Created by Manish on 1/14/2016.
 */
public class CMSAutoResizeTextView extends AutoResizeTextView {
    public CMSAutoResizeTextView(Context context) {
        super(context);
        TypefaceCache.setCustomTypeface(context, this, null);
    }

    public CMSAutoResizeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }

    public CMSAutoResizeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }
}