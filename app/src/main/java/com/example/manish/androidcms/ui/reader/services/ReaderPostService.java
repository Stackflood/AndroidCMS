package com.example.manish.androidcms.ui.reader.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.datasets.ReaderPostTable;
import com.example.manish.androidcms.datasets.ReaderTagTable;
import com.example.manish.androidcms.models.ReaderPostList;
import com.example.manish.androidcms.models.ReaderTag;
import com.example.manish.androidcms.models.ReaderTagType;
import com.example.manish.androidcms.ui.reader.ReaderConstants;
import com.example.manish.androidcms.ui.reader.ReaderEvents;
import com.example.manish.androidcms.ui.reader.actions.ReaderActions;
import com.example.manish.androidcms.ui.reader.utils.ReaderUtils;

import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

import java.io.Reader;

import Rest.RestRequest;
import de.greenrobot.event.EventBus;

/**
 * service which updates posts with specific tags or
 * in specific blogs/feeds - relies on
 * EventBus to alert of update status
 */
public class ReaderPostService extends Service {

    public static enum UpdateAction {REQUEST_NEWER, REQUEST_OLDER}

    private static final String ARG_TAG     = "tag";
    private static final String ARG_ACTION  = "action";
    private static final String ARG_BLOG_ID = "blog_id";
    private static final String ARG_FEED_ID = "feed_id";

    /*
     * update posts with the passed tag
     */
    public static void startServiceForTag(Context context, ReaderTag tag, UpdateAction action) {
        Intent intent = new Intent(context, ReaderPostService.class);
        intent.putExtra(ARG_TAG, tag);
        intent.putExtra(ARG_ACTION, action);
        context.startService(intent);
    }

    /*
     * update posts in the passed feed
     */
    public static void startServiceForFeed(Context context, long feedId, UpdateAction action)
    {
        Intent intent = new Intent(context, ReaderPostService.class);
        intent.putExtra(ARG_FEED_ID, feedId);
        intent.putExtra(ARG_ACTION, action);
        context.startService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //1.
    //The system calls this method when the service is first
    // created using onStartCommand() or onBind().
    //This call is required to perform one-time set-up.
    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.READER, "reader post service > created");
    }

    //3.The system calls this method when the service is no longer used
    // and is being destroyed. Your service should implement this to clean up
    // any resources such as threads, registered listeners, receivers, etc.
    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.READER, "reader post service > destroyed");
        super.onDestroy();
    }

    //2.

    //The system calls this method when another component,
    // such as an activity, requests that the service be started, by calling startService(). If you implement this method, it is your responsibility to stop the
    // service when its work is done, by calling stopSelf() or stopService() methods.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        UpdateAction action;
        if (intent.hasExtra(ARG_ACTION)) {
            action = (UpdateAction) intent.getSerializableExtra(ARG_ACTION);
        } else {
            action = UpdateAction.REQUEST_NEWER;
        }

        EventBus.getDefault().post(new ReaderEvents.UpdatePostsStarted(action));

        if (intent.hasExtra(ARG_TAG)) {
            ReaderTag tag = (ReaderTag) intent.getSerializableExtra(ARG_TAG);
            updatePostsWithTag(tag, action);
        } else if (intent.hasExtra(ARG_BLOG_ID)) {
            long blogId = intent.getLongExtra(ARG_BLOG_ID, 0);
            //updatePostsInBlog(blogId, action);
        } else if (intent.hasExtra(ARG_FEED_ID)) {
            long feedId = intent.getLongExtra(ARG_FEED_ID, 0);
            //updatePostsInFeed(feedId, action);
        }

        return START_NOT_STICKY;
    }

    void updatePostsWithTag(final ReaderTag tag, final UpdateAction action)
    {
        requestPostsWithTag
                (tag,
                        action,
                        new ReaderActions.UpdateResultListener() {
                            @Override
                            public void onUpdateResult(ReaderActions.UpdateResult result) {
                                EventBus.getDefault().post(new ReaderEvents.
                                        UpdatePostsEnded(tag, result, action));
                                stopSelf();
                            }
                        });
    }

    private static void requestPostsWithTag(final ReaderTag tag,
                                            final UpdateAction updateAction,
                                            final ReaderActions.UpdateResultListener resultListener)
    {
        String endpoint = getEndpointForTag(tag);

        if (TextUtils.isEmpty(endpoint)) {
            resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            return;
        }

        StringBuilder sb = new StringBuilder(endpoint);

        // append #posts to retrieve
        sb.append("?number=").append(ReaderConstants.READER_MAX_POSTS_TO_REQUEST);
        // return newest posts first (this is the default, but make it explicit since it's important)
        sb.append("&order=DESC");

        // if older posts are being requested, add the &before param based on the oldest existing post
        if (updateAction == UpdateAction.REQUEST_OLDER) {
            String dateOldest = ReaderPostTable.getOldestPubDateWithTag(tag);
            if (!TextUtils.isEmpty(dateOldest)) {
                sb.append("&before=").append(UrlUtils.urlEncode(dateOldest));
            }
        }

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                // remember when this tag was updated if newer posts were requested
                if (updateAction == UpdateAction.REQUEST_NEWER) {
                    ReaderTagTable.setTagLastUpdated(tag);
                }
                handleUpdatePostsResponse(tag, jsonObject, resultListener);
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.READER, volleyError);
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            }
        };

        CMS.getRestClientUtilsV1_1().get(sb.toString(), null, null, listener, errorListener);


    }


    /*
     * called after requesting posts with a specific tag or in a specific blog
     */
    private static void handleUpdatePostsResponse(final ReaderTag tag,
                                                  final JSONObject jsonObject,
                                                  final ReaderActions.UpdateResultListener resultListener)
    {
        if(jsonObject == null)
        {
            resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            return;
        }

        new Thread()
        {
            @Override
            public void run()
            {
                ReaderPostList serverPosts = ReaderPostList.fromJson(jsonObject);
                ReaderActions.UpdateResult updateResult = ReaderPostTable.comparePosts(serverPosts);
                if (updateResult.isNewOrChanged()) {
                    ReaderPostTable.addOrUpdatePosts(tag, serverPosts);
                }
                AppLog.d(AppLog.T.READER, "requested posts response = " + updateResult.toString());
                resultListener.onUpdateResult(updateResult);
            }
        }.start();
    }

    /*
     * returns the endpoint to use when requesting posts with the passed tag
     */
    private static String getEndpointForTag(ReaderTag tag) {
        if (tag == null) {
            return null;
        }

        // if passed tag has an assigned endpoint, return it and be done
        if (!TextUtils.isEmpty(tag.getEndpoint())) {
            return getRelativeEndpoint(tag.getEndpoint());
        }

        // check the db for the endpoint
        String endpoint = ReaderTagTable.getEndpointForTag(tag);
        if (!TextUtils.isEmpty(endpoint)) {
            return getRelativeEndpoint(endpoint);
        }

        // never hand craft the endpoint for default tags, since these MUST be updated
        // using their stored endpoints
        if (tag.tagType == ReaderTagType.DEFAULT) {
            return null;
        }

        return String.format("read/tags/%s/posts", ReaderUtils.sanitizeWithDashes(tag.getTagName()));
    }

    /*
     * returns the passed endpoint without the unnecessary path - this is
     * needed because as of 20-Feb-2015 the /read/menu/ call returns the
     * full path but we don't want to use the full path since it may change
     * between API versions (as it did when we moved from v1 to v1.1)
     *
     * ex: https://public-api.wordpress.com/rest/v1/read/tags/fitness/posts
     *     becomes just                             read/tags/fitness/posts
     */
    private static String getRelativeEndpoint(final String endpoint) {
        if (endpoint != null && endpoint.startsWith("http")) {
            int pos = endpoint.indexOf("/read/");
            if (pos > -1) {
                return endpoint.substring(pos + 1, endpoint.length());
            }
            pos = endpoint.indexOf("/v1/");
            if (pos > -1) {
                return endpoint.substring(pos + 4, endpoint.length());
            }
        }
        return StringUtils.notNullStr(endpoint);
    }

}
