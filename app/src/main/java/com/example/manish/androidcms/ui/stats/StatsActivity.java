package com.example.manish.androidcms.ui.stats;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.Toast;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.Blog;
import com.example.manish.androidcms.ui.CMSDrawerActivity;

import org.wordpress.android.analytics.AnalyticsTracker;

/**
 * The native stats activity, accessible via the menu drawer.
 * <p>
 * By pressing a spinner on the action bar, the user can select which timeframe they wish to see.
 * </p>
 */
public class StatsActivity extends CMSDrawerActivity {

    public static final String ARG_NO_MENU_DRAWER = "no_menu_drawer";

    public static final String ARG_LOCAL_TABLE_BLOG_ID = "ARG_LOCAL_TABLE_BLOG_ID";

    private boolean mNoMenuDrawer = false;
    private int mLocalBlogID = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (CMS.cmsDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_ACCESSED);
        }

        mNoMenuDrawer = getIntent().getBooleanExtra(ARG_NO_MENU_DRAWER, false);
        ActionBar actionBar = getSupportActionBar();

        createMenuDrawer(R.layout.stats_activity);

        if (mNoMenuDrawer) {
            getDrawerToggle().setDrawerIndicatorEnabled(false);
            // Override the default NavigationOnClickListener
            getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        setTitle(R.string.stats);

        //Make sure the blog_id passed to this activity is valid and the blog is available within the app
        final Blog currentBlog = CMS.getBlog(mLocalBlogID);

        loadStatsFragments(false, true, true);

    }

    private void loadStatsFragments(boolean forceRecreationOfFragments,
                                    boolean loadGraphFragment,
                                    boolean loadAlltimeFragmets) {

        if (isFinishing() || isActivityDestroyed()) {
            return;
        }

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        StatsAbstractFragment fragment;

        if(loadGraphFragment)
        {
            if(fm.findFragmentByTag(StatsVisitorsAndViewsFragment.TAG) == null ||
                    forceRecreationOfFragments)
            {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.GRAPH_AND_SUMMARY,
                        mLocalBlogID);

                ft.replace(R.id.stats_visitors_and_views_container,
                        fragment, StatsVisitorsAndViewsFragment.TAG);
            }
        }

        ft.commitAllowingStateLoss();

    }
}