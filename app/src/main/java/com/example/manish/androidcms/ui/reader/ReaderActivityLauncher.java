package com.example.manish.androidcms.ui.reader;

import android.content.Context;
import android.content.Intent;

import com.example.manish.androidcms.models.ReaderPost;

import org.wordpress.android.analytics.AnalyticsTracker;

/**
 * Created by Manish on 12/31/2015.
 */
public class ReaderActivityLauncher {

    /*
     * show a list of posts in a specific blog
     */
    public static void showReaderBlogPreview(Context context, long blogId)
    {
        if (blogId == 0) {
            return;
        }
        //AnalyticsTracker.track(AnalyticsTracker.Stat.READER_BLOG_PREVIEW);
        Intent intent = new Intent(context, ReaderPostListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderTypes.ReaderPostListType.BLOG_PREVIEW);
        context.startActivity(intent);
    }

    public static void showReaderFeedPreview(Context context, long feedId) {
        if (feedId == 0) {
            return;
        }
      //  AnalyticsTracker.track(AnalyticsTracker.Stat.READER_BLOG_PREVIEW);
        Intent intent = new Intent(context, ReaderPostListActivity.class);
        intent.putExtra(ReaderConstants.ARG_FEED_ID, feedId);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderTypes.ReaderPostListType.BLOG_PREVIEW);
        context.startActivity(intent);
    }

    public static void showReaderBlogPreview(Context context, ReaderPost post) {
        if (post == null) {
            return;
        }
        if (post.isExternal) {
            showReaderFeedPreview(context, post.feedId);
        } else {
            showReaderBlogPreview(context, post.blogId);
        }
    }
}
