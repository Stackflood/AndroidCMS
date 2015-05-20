package com.example.manish.androidcms.ui.comments;


import android.os.Handler;
import android.text.TextUtils;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.models.Blog;

import org.wordpress.android.util.AppLog;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import xmlrpc.android.XMLRPCClientInterface;
import xmlrpc.android.XMLRPCException;
import xmlrpc.android.XMLRPCFactory;

/**
 * actions related to comments - replies, moderating, etc.
 * methods below do network calls in the background & update local DB upon success
 * all methods below MUST be called from UI thread
 */

public class CommentActions {

    private CommentActions() {
        throw new AssertionError();
    }

    /*
     * listener when a comment action is performed
     */
    public interface CommentActionListener
    {
        public void onActionResult(boolean succeded);
    }


    /*
 * add a comment for the passed post
 */
    public static void addComment(final int accountId,
                                  final String postID,
                                  final String commentText,
                                  final CommentActionListener actionListener) {
        final Blog blog = CMS.getBlog(accountId);
        if (blog==null || TextUtils.isEmpty(commentText)) {
            if (actionListener != null)
                actionListener.onActionResult(false);
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(),
                        blog.getHttpuser(),
                        blog.getHttppassword());

                Map<String, Object> commentHash = new HashMap<String, Object>();
                commentHash.put("content", commentText);
                commentHash.put("author", "");
                commentHash.put("author_url", "");
                commentHash.put("author_email", "");

                Object[] params = {
                        blog.getRemoteBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        postID,
                        commentHash};

                int newCommentID;
                try {
                    newCommentID = (Integer) client.call("wp.newComment", params);
                } catch (XMLRPCException e) {
                    AppLog.e(AppLog.T.COMMENTS, "Error while sending new comment", e);
                    newCommentID = -1;
                } catch (IOException e) {
                    AppLog.e(AppLog.T.COMMENTS, "Error while sending new comment", e);
                    newCommentID = -1;
                } catch (XmlPullParserException e) {
                    AppLog.e(AppLog.T.COMMENTS, "Error while sending new comment", e);
                    newCommentID = -1;
                }

                final boolean succeeded = (newCommentID >= 0);

                if (actionListener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onActionResult(succeeded);
                        }
                    });
                }
            }
        }.start();
    }

}
