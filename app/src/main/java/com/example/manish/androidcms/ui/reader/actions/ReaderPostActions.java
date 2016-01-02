package com.example.manish.androidcms.ui.reader.actions;

import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.datasets.ReaderLikeTable;
import com.example.manish.androidcms.datasets.ReaderPostTable;
import com.example.manish.androidcms.models.ReaderPost;
import com.example.manish.androidcms.util.VolleyUtils;

import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import Rest.RestRequest;

/**
 * Created by Manish on 12/31/2015.
 */
public class ReaderPostActions {

    /**
     * like/unlike the passed post
     */
    public static boolean performLikeAction(final ReaderPost post,
                                            final boolean isAskingToLike) {
        // do nothing if post's like state is same as passed
        boolean isCurrentlyLiked = ReaderPostTable.isPostLikedByCurrentUser(post);
        if (isCurrentlyLiked == isAskingToLike) {
            AppLog.w(AppLog.T.READER, "post like unchanged");
            return false;
        }

        // update like status and like count in local db
        int newNumLikes = (isAskingToLike ? post.numLikes + 1 : post.numLikes - 1);
        ReaderPostTable.setLikesForPost(post, newNumLikes, isAskingToLike);
        ReaderLikeTable.setCurrentUserLikesPost(post, isAskingToLike);

        final String actionName = isAskingToLike ? "like" : "unlike";
        String path = "sites/" + post.blogId + "/posts/" + post.postId + "/likes/";
        if (isAskingToLike) {
            path += "new";
        } else {
            path += "mine/delete";
        }

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.d(AppLog.T.READER, String.format("post %s succeeded", actionName));
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                String error = VolleyUtils.errStringFromVolleyError(volleyError);
                if (TextUtils.isEmpty(error)) {
                    AppLog.w(AppLog.T.READER, String.format("post %s failed", actionName));
                } else {
                    AppLog.w(AppLog.T.READER, String.format("post %s failed (%s)", actionName, error));
                }
                AppLog.e(AppLog.T.READER, volleyError);
                ReaderPostTable.setLikesForPost(post, post.numLikes, post.isLikedByCurrentUser);
                ReaderLikeTable.setCurrentUserLikesPost(post, post.isLikedByCurrentUser);
            }
        };

        CMS.getRestClientUtilsV1_1().post(path, listener, errorListener);
        return true;
    }
}
