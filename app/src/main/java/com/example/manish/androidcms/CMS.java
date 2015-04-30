package com.example.manish.androidcms;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.example.manish.androidcms.datasets.SuggestionTable;
import com.example.manish.androidcms.models.Blog;
import com.example.manish.androidcms.models.Post;
import com.example.manish.androidcms.networking.OAuthAuthenticator;
import com.example.manish.androidcms.networking.OAuthAuthenticatorFactory;
import com.example.manish.androidcms.ui.analytics.AnalyticsTracker;
import com.example.manish.androidcms.util.BitmapLruCache;
import com.example.manish.androidcms.util.CoreEvents;
import com.example.manish.androidcms.util.HelpshiftHelper;
import com.example.manish.androidcms.util.VolleyUtils;
import Rest.RestRequest;


import NetWorking.RestClientUtils;
import org.wordpress.android.util.PackageUtils;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * Created by Manish on 4/1/2015.
 */


/*onCreate( ) Called when the application is starting, before any other application objects have been created.*/
public class CMS extends Application {

    public static final String ACCESS_TOKEN_PREFERENCE="wp_pref_wpcom_access_token";
    public static final String WPCOM_USERNAME_PREFERENCE="wp_pref_wpcom_username";
    public static final String IS_SIGNED_OUT_PREFERENCE="wp_pref_is_signed_out";
    public static String versionName;
    public static CMSDB cmsDB;

    public static Blog currentBlog;
    private static Context mContext;
    public static RequestQueue requestQueue;
    public static ImageLoader imageLoader;
    public static Post currentPost;
    public static RestClientUtils mRestClientUtils;
    public static RestClientUtils mRestClientUtilsVersion1_1;
    private static BitmapLruCache mBitmapCache;
    public static OnPostUploadedListener onPostUploadedListener = null;


    public void onCreate()
    {
        super.onCreate();
        mContext = this;
        ProfilingUtils.start("CMS.onCreate");
        // Enable log recording
        AppLog.enableRecording(true);
        if (!PackageUtils.isDebugBuild()) {
/*
            Fabric.with(this, new Crashlytics());
*/
        }

        versionName = PackageUtils.getVersionName(this);
        // HelpshiftHelper.init(this);

        initCMSDb();
        enableHttpResponseCache(mContext);
        // EventBus setup
        EventBus.TAG = "WordPress-EVENT";
        EventBus.builder()
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .throwSubscriberException(true)
                .installDefaultEventBus();


        RestClientUtils.setUserAgent(getUserAgent());



        // Volley networking setup
        setupVolleyQueue();

        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            AppLockManager.getInstance().getCurrentAppLock().setDisabledActivities(
                    new String[]{"org.wordpress.android.ui.ShareIntentReceiverActivity"});
        }

        registerForCloudMessaging(this);

        /*ApplicationLifecycleMonitor pnBackendMonitor = new ApplicationLifecycleMonitor();
        registerComponentCallbacks(pnBackendMonitor);
        registerActivityLifecycleCallbacks(pnBackendMonitor);*/

        // ABTestingUtils.init();
        // we want to reset the suggestion table in every launch so we can get a fresh list
        SuggestionTable.reset(cmsDB.getDatabase());

    }

    public static Blog setCurrentBlog(int id) {
        currentBlog = cmsDB.instantiateBlogByLocalId(id);
        return currentBlog;
    }

    /**
     * Set the last active blog as the current blog.
     *
     * @return the current blog
     */
    public static Blog setCurrentBlogToLastActive() {
        List<Map<String, Object>> accounts = CMS.cmsDB.getVisibleAccounts();

        int lastBlogId = CMS.cmsDB.getLastBlogId();
        if (lastBlogId != -1) {
            for (Map<String, Object> account : accounts) {
                int id = Integer.valueOf(account.get("id").toString());
                if (id == lastBlogId) {
                    setCurrentBlog(id);
                    return currentBlog;
                }
            }
        }
        // Previous active blog is hidden or deleted
        currentBlog = null;
        return null;
    }

    /**
     * Get the currently active blog.
     * <p/>
     * If the current blog is not already set, try and determine the last active blog from the last
     * time the application was used. If we're not able to determine the last active blog, just
     * select the first one.
     */
    public static Blog getCurrentBlog() {
        if (currentBlog == null || !cmsDB.isDotComAccountVisible(currentBlog.getRemoteBlogId())) {
            // attempt to restore the last active blog
            setCurrentBlogToLastActive();

            // fallback to just using the first blog
            List<Map<String, Object>> accounts = CMS.cmsDB.getVisibleAccounts();
            if (currentBlog == null && accounts.size() > 0) {
                int id = Integer.valueOf(accounts.get(0).get("id").toString());
                setCurrentBlog(id);
                cmsDB.updateLastBlogId(id);
            }
        }

        return currentBlog;
    }

    public interface OnPostUploadedListener {
        public abstract void OnPostUploaded(int localBlogId, String postId, boolean isPage);

        public abstract void OnPostUploadFailed(int localBlogId);
    }
    public static void setOnPostUploadedListener(OnPostUploadedListener listener) {
        onPostUploadedListener = listener;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }


    public static RestClientUtils getRestClientUtils() {
        if (mRestClientUtils == null) {
            OAuthAuthenticator authenticator = OAuthAuthenticatorFactory.instantiate();
            mRestClientUtils = new RestClientUtils(requestQueue, authenticator, mOnAuthFailedListener);
        }
        return mRestClientUtils;
    }

    public static int getCurrentLocalTableBlogId() {
        return (getCurrentBlog() != null ? getCurrentBlog().getLocalTableBlogId() : -1);
    }

    private static RestRequest.OnAuthFailedListener mOnAuthFailedListener = new RestRequest.OnAuthFailedListener() {
        @Override
        public void onAuthFailed() {
            if (getContext() == null) return;
            // If this is called, it means the WP.com token is no longer valid.
            EventBus.getDefault().post(new CoreEvents.RestApiUnauthorized());
        }
    };

    public static void setupVolleyQueue() {
        requestQueue = Volley.newRequestQueue(mContext, VolleyUtils.getHTTPClientStack(mContext));
        imageLoader = new ImageLoader(requestQueue, getBitmapCache());
        VolleyLog.setTag(AppLog.TAG);
        // http://stackoverflow.com/a/17035814
        imageLoader.setBatchedResponseDelay(0);
    }

    public static BitmapLruCache getBitmapCache() {
        if (mBitmapCache == null) {
            // The cache size will be measured in kilobytes rather than
            // number of items. See http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html
            int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
            int cacheSize = maxMemory / 16;  //Use 1/16th of the available memory for this memory cache.
            mBitmapCache = new BitmapLruCache(cacheSize);
        }
        return mBitmapCache;
    }

    /**
     * User-Agent string when making HTTP connections, for both API traffic and WebViews.
     * Follows the format detailed at http://tools.ietf.org/html/rfc2616#section-14.43,
     * ie: "AppName/AppVersion (OS Version; Locale; Device)"
     *    "wp-android/2.6.4 (Android 4.3; en_US; samsung GT-I9505/jfltezh)"
     *    "wp-android/2.6.3 (Android 4.4.2; en_US; LGE Nexus 5/hammerhead)"
     * Note that app versions prior to 2.7 simply used "wp-android" as the user agent
     **/
    private static final String USER_AGENT_APPNAME = "wp-android";
    private static String mUserAgent;
    public static String getUserAgent() {
        if (mUserAgent == null) {
            mUserAgent = USER_AGENT_APPNAME + "/" + PackageUtils.getVersionName(getContext())
                    + " (Android " + Build.VERSION.RELEASE + "; "
                    + Locale.getDefault().toString() + "; "
                    + Build.MANUFACTURER + " " + Build.MODEL + "/" + Build.PRODUCT + ")";
        }
        return mUserAgent;
    }

    /**
     * Returns WordPress.com Auth Token
     *
     * @return String - The wpcom Auth token, or null if not authenticated.
     */
    public static String getWPComAuthToken(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(CMS.ACCESS_TOKEN_PREFERENCE, null);

    }

    public static void registerForCloudMessaging(Context context) {
        /*String regId = gcmRegisterIfNot(context);

        // Register to WordPress.com notifications
        if (WordPress.hasDotComToken(context)) {
            if (!TextUtils.isEmpty(regId)) {
                // Send the token to WP.com in case it was invalidated
                NotificationsUtils.registerDeviceForPushNotifications(context, regId);
                AppLog.v(AppLog.T.NOTIFS, "Already registered for GCM");
            }
        }

        // Register to Helpshift notifications
        if (ABTestingUtils.isFeatureEnabled(Feature.HELPSHIFT)) {
            HelpshiftHelper.getInstance().registerDeviceToken(context, regId);
        }
        AnalyticsTracker.registerPushNotificationToken(regId);*/
    }

    /**
     * Get the blog with the specified ID.
     *
     * @param id ID of the blog to retrieve.
     * @return the blog with the specified ID, or null if blog could not be retrieved.
     */
    public static Blog getBlog(int id) {
        try {
            return cmsDB.instantiateBlogByLocalId(id);
        } catch (Exception e) {
            return null;
        }
    }

    /*
     * enable caching for HttpUrlConnection
     * http://developer.android.com/training/efficient-downloads/redundant_redundant.html
     */
    private static void enableHttpResponseCache(Context context) {
        try {
            long httpCacheSize = 5 * 1024 * 1024; // 5MB
            File httpCacheDir = new File(context.getCacheDir(), "http");
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            AppLog.w(AppLog.T.UTILS, "Failed to enable http response cache");
        }
    }

   private void initCMSDb() {
        if (!createAndVerifyCMSDb()) {
            AppLog.e(AppLog.T.DB, "Invalid database, sign out user and delete database");
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            currentBlog = null;
            editor.remove(CMS.WPCOM_USERNAME_PREFERENCE);
            editor.remove(CMS.ACCESS_TOKEN_PREFERENCE);
            editor.commit();
            if (cmsDB != null) {
                cmsDB.updateLastBlogId(-1);
            }
            // Force DB deletion
            CMSDB.deleteDatabase(this);
            cmsDB = new CMSDB(this);
        }
    }

    public static boolean isSignedIn(Context context) {
        if (CMS.hasDotComToken(context)) {
            return true;
        }
        return CMS.cmsDB.getNumVisibleAccounts() != 0;
    }

    private boolean createAndVerifyCMSDb() {
        try {
            CMSDB.deleteDatabase(this);
            cmsDB = new CMSDB(this);
            // verify account data
            List<Map<String, Object>> accounts = cmsDB.getAllAccounts();
            for (Map<String, Object> account : accounts) {
                if (account == null || account.get("blogName") == null || account.get("url") == null) {
                    return false;
                }
            }
            return true;
        } catch (SQLiteException sqle) {
            AppLog.e(AppLog.T.DB, sqle);
            return false;
        } catch (RuntimeException re) {
            AppLog.e(AppLog.T.DB, re);
            return false;
        }
    }

    public static Context getContext() {
        return mContext;
    }

    /**
     * Checks for WordPress.com credentials
     *
     * @return true if we have credentials or false if not
     */
    public static boolean hasDotComToken(Context context) {
        if (context == null) return false;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return !TextUtils.isEmpty(settings.getString(ACCESS_TOKEN_PREFERENCE, null));
    }
}
/*ig99895*/
