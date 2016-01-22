package com.example.manish.androidcms.ui.stats;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;

/**
 * Timeframes for the stats pages.
 */
public enum StatsTimeframe {
    DAY(R.string.stats_timeframe_days),
    WEEK(R.string.stats_timeframe_weeks),
    MONTH(R.string.stats_timeframe_months),
    YEAR(R.string.stats_timeframe_years),
    ;

    private final int mLabelResId;

    private StatsTimeframe(int labelResId) {
        mLabelResId = labelResId;
    }

    public String getLabel() {
        return CMS.getContext().getString(mLabelResId);
    }

    public String getLabelForRestCall() {
        switch (this) {
            case WEEK:
                return "week";
            case MONTH:
                return "month";
            case YEAR:
                return "year";
            default:
                return "day";
        }
    }

    public static String[] toStringArray(StatsTimeframe[] timeframes) {
        String[] titles = new String[timeframes.length];

        for (int i = 0; i < timeframes.length; i++) {
            titles[i] = timeframes[i].getLabel();
        }

        return titles;
    }
}