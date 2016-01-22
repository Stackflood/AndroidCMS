package com.example.manish.androidcms.ui.stats;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;

import java.io.Serializable;

/**
 * Created by Manish on 1/5/2016.
 */
public class StatsVisitorsAndViewsFragment extends StatsAbstractFragment
    implements StatsBarGraph.OnGestureListener
{


    private LinearLayout mGraphContainer;
    private StatsBarGraph mGraphView;
    private LinearLayout mModuleButtonsContainer;
    private TextView mDateTextView;
    private String[] mStatsDate;

    private LinearLayout mLegendContainer;
    private CheckedTextView mLegendLabel;
    private LinearLayout mVisitorsCheckboxContainer;
    private CheckBox mVisitorsCheckbox;
    private boolean mIsCheckboxChecked;


    // Restore the following variables on restart
    private Serializable mVisitsData; //VisitModel or VolleyError
    private int mSelectedOverviewItemIndex = 0;
    private int mSelectedBarGraphBarIndex = -1;
    private OnDateChangeListener mListener;

    final OverviewLabel[] overviewItems = {OverviewLabel.VIEWS,
            OverviewLabel.VISITORS, OverviewLabel.LIKES,
            OverviewLabel.COMMENTS};

    private enum OverviewLabel {
        VIEWS(R.string.stats_views),
        VISITORS(R.string.stats_visitors),
        LIKES(R.string.stats_likes),
        COMMENTS(R.string.stats_comments),
        ;

        private final int mLabelResId;

        OverviewLabel(int labelResId) {
            mLabelResId = labelResId;
        }

        public String getLabel() {
            return CMS.getContext().getString(mLabelResId).toUpperCase();
        }

        public static String[] toStringArray(OverviewLabel[] timeframes) {
            String[] titles = new String[timeframes.length];

            for (int i = 0; i < timeframes.length; i++) {
                titles[i] = timeframes[i].getLabel();
            }

            return titles;
        }
    }

    public static final String TAG = StatsVisitorsAndViewsFragment.class.getSimpleName();

    // Container Activity must implement this interface
    public interface OnDateChangeListener {
        void onDateChanged(String blogID, StatsTimeframe timeframe, String newDate);
    }


    //2.After this step onAttach is called.
    // This method is called as soon as the fragment is “attached”
    // to the “father” activity and we can this method to
    // store the reference about the activity.

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnDateChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnDateChangeListener");
        }
    }

    //3.onCreate :
    //After it we have onCreate. It is one of the most important step,
    // our fragment is in the creation process. This method
    //can be used to start some thread to retrieve data information,
    // maybe from a remote server.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {

        }
    }

    //4.onCreateView
    //The onCreateView is the method called when the fragment has
    // to create its view hierarchy. During this method we will inflate our
    // layout inside the fragment as we do for example in the ListView widget.
    // During this phase we
    //can’t be sure that our activity is still created so we can’t count on it for some operation.
    @Override
    public View onCreateView(LayoutInflater inflater,
                            ViewGroup container,
                            Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.stats_visitors_and_views_fragment,
                container, false);

        mDateTextView = (TextView) view.findViewById(R.id.stats_summary_date);
        mGraphContainer = (LinearLayout) view.findViewById(R.id.stats_bar_chart_fragment_container);
        mModuleButtonsContainer = (LinearLayout) view.findViewById(R.id.stats_pager_tabs);

        mLegendContainer = (LinearLayout) view.findViewById(R.id.stats_legend_container);
        mLegendLabel = (CheckedTextView) view.findViewById(R.id.stats_legend_label);
        mLegendLabel.setCheckMarkDrawable(null); // Make sure to set a null drawable here. Otherwise the touching area is the same of a TextView
        mVisitorsCheckboxContainer = (LinearLayout) view.findViewById(R.id.stats_checkbox_visitors_container);
        mVisitorsCheckbox = (CheckBox) view.findViewById(R.id.stats_checkbox_visitors);
        mVisitorsCheckbox.setOnClickListener(onCheckboxClicked);

        // Fix an issue on devices with 4.1 or lower, where the Checkbox already uses padding by default internally and overriding it with paddingLeft
        // causes the issue report here https://github.com/wordpress-mobile/WordPress-Android/pull/2377#issuecomment-77067993
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mVisitorsCheckbox.setPadding(getResources().getDimensionPixelSize(R.dimen.margin_medium), 0, 0, 0);
        }

        // Make sure we've all the info to build the tab correctly.
        // This is ALWAYS true
        if (mModuleButtonsContainer.getChildCount() == overviewItems.length) {
            for (int i = 0; i < mModuleButtonsContainer.getChildCount(); i++) {

                LinearLayout currentTab = (LinearLayout) mModuleButtonsContainer.
                        getChildAt(i);

                boolean isLastItem = i == (overviewItems.length - 1);
                boolean isChecked = i == mSelectedOverviewItemIndex;
                TabViewHolder currentTabViewHolder = new TabViewHolder
                        (currentTab, overviewItems[i], isChecked, isLastItem);

                currentTab.setOnClickListener(TopButtonsOnClickListener);
                currentTab.setTag(currentTabViewHolder);
            }

            mModuleButtonsContainer.setVisibility(View.VISIBLE);
        }

        return view;
    }

    private View.OnClickListener TopButtonsOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    private class TabViewHolder {
    LinearLayout tab;
    LinearLayout innerContainer;
    TextView label;
    TextView value;
    ImageView icon;
    OverviewLabel labelItem;
    boolean isChecked = false;
    boolean isLastItem = false;

    public TabViewHolder(LinearLayout currentTab, OverviewLabel labelItem, boolean checked, boolean isLastItem) {
        tab = currentTab;
        innerContainer = (LinearLayout) currentTab.findViewById(R.id.stats_visitors_and_views_tab_inner_container);
        label = (TextView) currentTab.findViewById(R.id.stats_visitors_and_views_tab_label);
        label.setText(labelItem.getLabel());
        value = (TextView) currentTab.findViewById(R.id.stats_visitors_and_views_tab_value);
        icon = (ImageView) currentTab.findViewById(R.id.stats_visitors_and_views_tab_icon);
        this.labelItem = labelItem;
        this.isChecked = checked;
        this.isLastItem = isLastItem;
        updateBackGroundAndIcon();
    }

    private Drawable getTabIcon() {
        switch (labelItem) {
            case VISITORS:
                return isChecked ? getResources().getDrawable(R.drawable.stats_icon_visitors_active) :
                        getResources().getDrawable(R.drawable.stats_icon_visitors);
            case COMMENTS:
                return isChecked ? getResources().getDrawable(R.drawable.stats_icon_comments_active) :
                        getResources().getDrawable(R.drawable.stats_icon_comments);
            case LIKES:
                return isChecked ? getResources().getDrawable(R.drawable.stats_icon_likes_active) :
                        getResources().getDrawable(R.drawable.stats_icon_likes);
            default:
                // Views and when no prev match
                return isChecked ? getResources().getDrawable(R.drawable.stats_icon_views_active) :
                        getResources().getDrawable(R.drawable.stats_icon_views);
        }
    }

    public void updateBackGroundAndIcon() {
        if (isChecked) {
            label.setTextColor(getResources().getColor(R.color.grey_dark));
            value.setTextColor(getResources().getColor(R.color.orange_jazzy));
        } else {
            label.setTextColor(getResources().getColor(R.color.grey_darken_20));
            value.setTextColor(getResources().getColor(R.color.blue_wordpress));
        }

        icon.setImageDrawable(getTabIcon());

        if (isLastItem) {
            if (isChecked) {
                tab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_latest_white);
            } else {
                tab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_latest_blue_light);
            }
        } else {
            if (isChecked) {
                tab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_white);
            } else {
                tab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_blue_light);
            }
        }
    }

    public void setChecked(boolean checked) {
        this.isChecked = checked;
    }
}


    View.OnClickListener onCheckboxClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Is the view now checked?
            mIsCheckboxChecked = ((CheckBox) view).isChecked();
            updateUI();
        }


        private void updateUI() {

        }
    };


    public void onBarTapped(int tappedBar)
    {

    }

}
