package com.example.manish.androidcms.ui.stats;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.example.manish.androidcms.R;
import com.example.manish.androidcms.widgets.TypefaceCache;
import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.IndexDependentColor;

import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Manish on 1/5/2016.
 */
public class StatsBarGraph extends GraphView {


    // Keep tracks of every bar drawn on the graph.
    private List<List<BarChartRect>> mSeriesRectsDrawedOnScreen = (List<List<BarChartRect>>) new LinkedList();

    private int mBarPositionToHighlight = -1;
    private GestureDetectorCompat mDetector;
    private OnGestureListener mGestureListener;

    @Override
    protected double getMinY() {
        return 0;
    }

    public StatsBarGraph(Context context) {
        super(context, "");

        int width = LayoutParams.MATCH_PARENT;
        int height = getResources().getDimensionPixelSize(R.dimen.stats_barchart_height);
        setLayoutParams(new LayoutParams(width, height));

        setProperties();

        // Use Open Sans
        paint.setTypeface(TypefaceCache.getTypeface(getContext()));

        mDetector = new GestureDetectorCompat(getContext(), new MyGestureListener());
        mDetector.setIsLongpressEnabled(false);
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            highlightBarAndBroadcastDate();
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            highlightBarAndBroadcastDate();
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        private void highlightBarAndBroadcastDate() {
            int tappedBar = getTappedBar();
            //AppLog.d(AppLog.T.STATS, this.getClass().getName() + " Tapped bar " + tappedBar);
            if (tappedBar >= 0) {
                highlightBar(tappedBar);
                if (mGestureListener != null) {
                    mGestureListener.onBarTapped(tappedBar);
                }
            }
        }
    }

    public void highlightBar(int barPosition) {
        mBarPositionToHighlight = barPosition;
        this.redrawAll();
    }

    public int getTappedBar() {
        float[] lastBarChartTouchedPoint = this.getLastTouchedPointOnCanvasAndReset();
        if (lastBarChartTouchedPoint[0] == 0f && lastBarChartTouchedPoint[1] == 0f) {
            return -1;
        }
        for (List<BarChartRect> currentSerieChartRects : mSeriesRectsDrawedOnScreen) {
            int i = 0;
            for (BarChartRect barChartRect : currentSerieChartRects) {
                if (barChartRect.isPointInside(lastBarChartTouchedPoint[0], lastBarChartTouchedPoint[1])) {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }

    private class HorizontalLabelsColor implements IndexDependentColor {
        public int get(int index) {
            if (mBarPositionToHighlight == index) {
                return getResources().getColor(R.color.orange_jazzy);
            } else {
                return getResources().getColor(R.color.grey_darken_30);
            }
        }
    }

    private void setProperties() {
        GraphViewStyle gStyle =  getGraphViewStyle();
        gStyle.setHorizontalLabelsIndexDependentColor(new HorizontalLabelsColor());
        gStyle.setHorizontalLabelsColor(getResources().getColor(R.color.grey_darken_30));
        gStyle.setVerticalLabelsColor(getResources().getColor(R.color.grey_darken_10));
        gStyle.setTextSize(getResources().getDimensionPixelSize(R.dimen.text_sz_extra_small));
        gStyle.setGridXColor(Color.TRANSPARENT);
        gStyle.setGridYColor(getResources().getColor(R.color.grey_lighten_30));
        gStyle.setNumVerticalLabels(3);

        setCustomLabelFormatter(new CustomLabelFormatter() {
            private NumberFormat numberFormatter;

            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    return null;
                }
                if (numberFormatter == null) {
                    numberFormatter = NumberFormat.getNumberInstance();
                    numberFormatter.setMaximumFractionDigits(0);
                }
                return numberFormatter.format(value);
            }
        });
    }

    @Override
    protected void onBeforeDrawSeries() {
        mSeriesRectsDrawedOnScreen.clear();
    }

    @Override
    public void drawSeries(Canvas canvas, GraphViewDataInterface[] values,
                           float graphwidth, float graphheight, float border, double minX,
                           double minY, double diffX, double diffY, float horstart,
                           GraphViewSeries.GraphViewSeriesStyle style) {
        float colwidth = graphwidth / values.length;
        int maxColumnSize = getGraphViewStyle().getMaxColumnWidth();
        if (maxColumnSize > 0 && colwidth > maxColumnSize) {
            colwidth = maxColumnSize;
        }

        paint.setStrokeWidth(style.thickness);
        paint.setColor(style.color);

        // Bar chart position of this series on the canvas
        List<BarChartRect> barChartRects = new LinkedList<>();

        // draw data
        for (int i = 0; i < values.length; i++) {
            float valY = (float) (values[i].getY() - minY);
            float ratY = (float) (valY / diffY);
            float y = graphheight * ratY;

            // hook for value dependent color
            if (style.getValueDependentColor() != null) {
                paint.setColor(style.getValueDependentColor().get(values[i]));
            }

            float pad = style.padding;

            float left = (i * colwidth) + horstart;
            float top = (border - y) + graphheight;
            float right = left + colwidth;
            float bottom = graphheight + border - 1;

            // Draw the orange selection behind the selected bar
            if (mBarPositionToHighlight == i && style.outerhighlightColor != 0x00ffffff) {
                paint.setColor(style.outerhighlightColor);
                canvas.drawRect(left, 10f, right, bottom, paint);
            }

            if ((top - bottom) == 1) {
                // draw a placeholder
                if (mBarPositionToHighlight != i) {
                    paint.setColor(style.color);
                    paint.setAlpha(25);
                    Shader shader = new LinearGradient(left + pad, bottom - 50, left + pad, bottom, Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP);
                    paint.setShader(shader);
                    canvas.drawRect(left + pad, bottom - 50, right - pad, bottom, paint);
                    paint.setShader(null);
                }
            } else {
                // draw a real bar
                paint.setAlpha(255);
                if (mBarPositionToHighlight == i) {
                    paint.setColor(style.highlightColor);
                } else {
                    paint.setColor(style.color);
                }
                canvas.drawRect(left + pad, top, right - pad, bottom, paint);
            }

            barChartRects.add(new BarChartRect(left + pad, top, right - pad, bottom));
        }
        mSeriesRectsDrawedOnScreen.add(barChartRects);
    }

    /**
     * Private class that is used to hold the local (to the canvas) coordinate on the screen
     * of every single bar in the graph
     */
    private class BarChartRect {
        float mLeft, mTop, mRight, mBottom;

        BarChartRect(float left, float top, float right, float bottom) {
            this.mLeft = left;
            this.mTop = top;
            this.mRight = right;
            this.mBottom = bottom;
        }

        /**
         * Check if the tap happens on a bar in the graph.
         *
         * @return true if the tap point falls within the bar for the X coordinate, and within the full canvas
         * height for the Y coordinate. This is a fix to make very small bars tappable.
         */
        public boolean isPointInside(float x, float y) {
            return x >= this.mLeft
                    && x <= this.mRight;
        }
    }

    interface OnGestureListener {
        public void onBarTapped(int tappedBar);
    }

}
