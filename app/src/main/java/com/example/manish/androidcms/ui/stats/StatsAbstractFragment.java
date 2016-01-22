package com.example.manish.androidcms.ui.stats;

import android.app.Fragment;
import android.os.Bundle;

/**
 * Created by Manish on 1/5/2016.
 */
public class StatsAbstractFragment extends Fragment {

    protected static final String ARGS_VIEW_TYPE = "ARGS_VIEW_TYPE";

    public static StatsAbstractFragment newInstance(StatsViewType viewType,
                                                    int localTableBlogID)
    {
        StatsAbstractFragment fragment = null;

        switch (viewType) {
            case GRAPH_AND_SUMMARY:
                fragment = new StatsVisitorsAndViewsFragment();
                break;
        }

        Bundle args = new Bundle();
        args.putInt(ARGS_VIEW_TYPE, viewType.ordinal());
        args.putInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, localTableBlogID);
        fragment.setArguments(args);

        return fragment;
    }
}
