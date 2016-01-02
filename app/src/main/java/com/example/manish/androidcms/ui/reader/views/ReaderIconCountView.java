package com.example.manish.androidcms.ui.reader.views;

import android.content.ContentValues;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.manish.androidcms.R;
import com.example.manish.androidcms.ui.reader.actions.ReaderAnim;

import org.wordpress.android.util.FormatUtils;

/*
 * used when showing comment + comment count, like + like count
 */
public class ReaderIconCountView extends LinearLayout {

    private ImageView mImageView;
    private TextView mTextCount;
    private int mCurrentCount;

    // these must match the same values in attrs.xml
    private static final int ICON_LIKE = 0;
    private static final int ICON_COMMENT = 1;

    public ReaderIconCountView(Context context){
        super(context);
        initView(context, null);
    }

    public ReaderIconCountView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public ReaderIconCountView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs)
    {
        inflate(context, R.layout.reader_icon_count_view, this);

        mImageView = (ImageView)findViewById(R.id.image_count);
        mTextCount = (TextView) findViewById(R.id.text_count);


    }

    public void setCount(int count, boolean animateChanges) {
        if (count != 0) {
            mTextCount.setText(FormatUtils.formatInt(count));
        }

        if (animateChanges && count != mCurrentCount) {
            if (count == 0 && mTextCount.getVisibility() == View.VISIBLE) {
                ReaderAnim.scaleOut(mTextCount, View.GONE, ReaderAnim.Duration.LONG, null);
            } else if (mCurrentCount == 0 && mTextCount.getVisibility() != View.VISIBLE) {
                ReaderAnim.scaleIn(mTextCount, ReaderAnim.Duration.LONG);
            } else {
                mTextCount.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            }
        } else {
            mTextCount.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        }

        mCurrentCount = count;
    }

    public ImageView getImageView() {
        return mImageView;
    }

    public void setSelected(boolean selected) {
        mImageView.setSelected(selected);
    }
}
