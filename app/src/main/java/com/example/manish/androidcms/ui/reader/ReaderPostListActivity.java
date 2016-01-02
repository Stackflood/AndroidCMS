package com.example.manish.androidcms.ui.reader;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.manish.androidcms.R;
import com.example.manish.androidcms.datasets.ReaderTagTable;
import com.example.manish.androidcms.models.ReaderTag;
import com.example.manish.androidcms.ui.CMSDrawerActivity;
import com.example.manish.androidcms.ui.prefs.AppPrefs;

/**
 * Created by Manish on 12/29/2015.
 */
public class ReaderPostListActivity extends CMSDrawerActivity {

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        createMenuDrawer(R.layout.reader_activity_post_list);
        readIntent(getIntent(), savedInstanceState);

    }

    @Override
    public void onBackPressed() {
        ReaderPostListFragment fragment = getListFragment();
        if (fragment == null || !fragment.goBackInTagHistory()) {
            setToolbarClickListener();
            super.onBackPressed();
        }
    }

    private ReaderPostListFragment getListFragment() {
        Fragment fragment = getFragmentManager().findFragmentByTag(getString(R.string.fragment_tag_reader_post_list));
        if (fragment == null) {
            return null;
        }
        return ((ReaderPostListFragment) fragment);
    }

    private void readIntent(Intent intent, Bundle savedInstanceState)
    {
        if (intent == null) {
            return;
        }

        ReaderTypes.ReaderPostListType postListType;

        if(intent.hasExtra(ReaderConstants.ARG_POST_LIST_TYPE))
        {
            postListType =
                    (ReaderTypes.ReaderPostListType)
                    intent.getSerializableExtra(ReaderConstants.ARG_POST_LIST_TYPE);
        }
        else
        {
            postListType = ReaderTypes.DEFAULT_POST_LIST_TYPE;
        }

        // hide drawer toggle and enable back arrow click if this is blog preview or tag preview
        if (postListType.isPreviewType() && getDrawerToggle() != null) {
            getDrawerToggle().setDrawerIndicatorEnabled(false);
            getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        if(savedInstanceState == null)
        {
            if (postListType == ReaderTypes.ReaderPostListType.BLOG_PREVIEW) {
                long blogId = intent.getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
                long feedId = intent.getLongExtra(ReaderConstants.ARG_FEED_ID, 0);
                if (feedId != 0) {
                    showListFragmentForFeed(feedId);
                } else {
                    showListFragmentForBlog(blogId);
                }
            }
            else
            {
                ReaderTag tag;
                if(intent.hasExtra(ReaderConstants.ARG_TAG))
                {
                    tag = (ReaderTag) intent.getSerializableExtra(ReaderConstants.ARG_TAG);
                }
                else
                {
                    tag = AppPrefs.getReaderTag();
                }

                // if this is a followed tag and it doesn't exist, revert to default tag
                if (postListType == ReaderTypes.ReaderPostListType.TAG_FOLLOWED &&
                        !ReaderTagTable.tagExists(tag)) {
                    tag = ReaderTag.getDefaultTag();
                }

                showListFragmentForTag(tag, postListType);
            }
        }

        switch (postListType) {
            case TAG_PREVIEW:
                setTitle(R.string.reader_title_tag_preview);
                break;
            case BLOG_PREVIEW:
                setTitle(R.string.reader_title_blog_preview);
                break;
            default:
                break;
        }

        // hide the static drawer for blog/tag preview
        if (isStaticMenuDrawer() && postListType.isPreviewType()) {
            hideDrawer();
        }
    }

    /*
   * show fragment containing list of latest posts in a specific blog
   */
    private void showListFragmentForBlog(long blogId) {
        if (isFinishing()) {
            return;
        }
        Fragment fragment = ReaderPostListFragment.newInstanceForBlog(blogId);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }


    private void showListFragmentForFeed(long feedId)
    {
        if(isFinishing())
        {
            return;
        }

        Fragment fragment = ReaderPostListFragment.newInstanceForFeed(feedId);

        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }

    /*
     * show fragment containing list of latest posts for a specific tag
     */
    private void showListFragmentForTag(final ReaderTag tag, ReaderTypes.ReaderPostListType listType) {
        if (isFinishing()) {
            return;
        }
        Fragment fragment = ReaderPostListFragment.newInstanceForTag(tag, listType);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container,
                        fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }

}
