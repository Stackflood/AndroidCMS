package com.example.manish.androidcms.ui.reader.actions;

import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.datasets.ReaderBlogTable;
import com.example.manish.androidcms.datasets.ReaderPostTable;
import com.example.manish.androidcms.models.ReaderBlog;
import com.example.manish.androidcms.models.ReaderPost;

import org.json.JSONObject;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.UrlUtils;

import Rest.RestRequest;

/**
 * Created by Manish on 12/31/2015.
 */
public class ReaderBlogActions {

    /*
     * helper routine when following a blog from a post view
     */

    public static boolean followBlogForPost(ReaderPost post,
                                            boolean isAskingToFollow,
                                            ReaderActions.ActionListener actionListener)
    {
        if (post == null) {
            AppLog.w(AppLog.T.READER, "follow action performed with null post");
            if (actionListener != null) {
                actionListener.onActionResult(false);
            }
            return false;
        }

        if (post.feedId != 0) {
            return followFeedById(post.feedId, isAskingToFollow, actionListener);
        } else {
            return true;
          //  return followBlogById(post.blogId, isAskingToFollow, actionListener);
        }
    }

    public static boolean followFeedById(final long feedId,
                                         final boolean isAskingToFollow,
                                         final ReaderActions.ActionListener actionListener)
    {
        ReaderBlog blogInfo = ReaderBlogTable.getFeedInfo(feedId);

        if (blogInfo != null) {
            return internalFollowFeed(blogInfo.feedId, blogInfo.getFeedUrl(), isAskingToFollow, actionListener);
        }

        /*updateFeedInfo(feedId, null, new UpdateBlogInfoListener() {
            @Override
            public void onResult(ReaderBlog blogInfo) {
                if (blogInfo != null) {
                    internalFollowFeed(
                            blogInfo.feedId,
                            blogInfo.getFeedUrl(),
                            isAskingToFollow,
                            actionListener);
                } else if (actionListener != null) {
                    actionListener.onActionResult(false);
                }
            }
        });*/

        return true;
    }

    /*
     * returns whether a follow/unfollow was successful based on the response to:
     *      read/follows/new
     *      read/follows/delete
     *      site/$site/follows/new
     *      site/$site/follows/mine/delete
     */
    private static boolean isFollowActionSuccessful(JSONObject json, boolean isAskingToFollow) {
        if (json == null) {
            return false;
        }

        final boolean isSubscribed;
        if (json.has("subscribed")) {
            // read/follows/
            isSubscribed = json.optBoolean("subscribed", false);
        } else if (json.has("is_following")) {
            // site/$site/follows/
            isSubscribed = json.optBoolean("is_following", false);
        } else {
            isSubscribed = false;
        }

        return (isSubscribed == isAskingToFollow);
    }

    private static String jsonToString(JSONObject json) {
        return (json != null ? json.toString() : "");
    }

    private static boolean internalFollowFeed(
            final long feedId,
            final String feedUrl,
            final boolean isAskingToFollow,
            final ReaderActions.ActionListener actionListener)
    {
        // feedUrl is required
        if (TextUtils.isEmpty(feedUrl)) {
            if (actionListener != null) {
                actionListener.onActionResult(false);
            }
            return false;
        }

        if (feedId != 0) {
            ReaderBlogTable.setIsFollowedFeedId(feedId, isAskingToFollow);
            ReaderPostTable.setFollowStatusForPostsInFeed(feedId, isAskingToFollow);
        }

        if (isAskingToFollow) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_FOLLOWED_SITE);
        }

        final String actionName = (isAskingToFollow ? "follow" : "unfollow");
        final String path = "read/following/mine/"
                + (isAskingToFollow ? "new" : "delete")
                + "?url=" + UrlUtils.urlEncode(feedUrl);

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                boolean success = isFollowActionSuccessful(jsonObject, isAskingToFollow);
                if (success) {
                    AppLog.d(AppLog.T.READER, "feed " + actionName + " succeeded");
                } else {
                    AppLog.w(AppLog.T.READER, "feed " + actionName + " failed - " + jsonToString(jsonObject) + " - " + path);
                    localRevertFollowFeedId(feedId, isAskingToFollow);
                }
                if (actionListener != null) {
                    actionListener.onActionResult(success);
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.w(AppLog.T.READER, "feed " + actionName + " failed with error");
                AppLog.e(AppLog.T.READER, volleyError);
                localRevertFollowFeedId(feedId, isAskingToFollow);
                if (actionListener != null) {
                    actionListener.onActionResult(false);
                }
            }
        };
        CMS.getRestClientUtilsV1_1().post(path, listener, errorListener);

        return true;
    }

    private static void localRevertFollowFeedId(long feedId, boolean isAskingToFollow) {
        ReaderBlogTable.setIsFollowedFeedId(feedId, !isAskingToFollow);
        ReaderPostTable.setFollowStatusForPostsInFeed(feedId, !isAskingToFollow);
    }
}
