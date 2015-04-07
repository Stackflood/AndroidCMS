package com.example.manish.androidcms;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.net.http.HttpResponseCache;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import com.example.manish.androidcms.datasets.SuggestionTable;
import com.example.manish.androidcms.models.Blog;
import com.example.manish.androidcms.models.Post;
import com.example.manish.androidcms.util.HelpshiftHelper;


import org.wordpress.android.util.PackageUtils;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.AppLog;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * Created by Manish on 4/1/2015.
 */


/*onCreate( ) Called when the application is starting, before any other application objects have been created.*/
public class CMS extends Application {

    public static final String ACCESS_TOKEN_PREFERENCE="wp_pref_wpcom_access_token";
    public static final String WPCOM_USERNAME_PREFERENCE="wp_pref_wpcom_username";
    public static String versionName;
    public static CMSDB cmsDB;

    public static Blog currentBlog;
    private static Context mContext;
    public static Post currentPost;

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

    public void onCreate()
    {
        super.onCreate();
        mContext = this;
        ProfilingUtils.start("WordPress.onCreate");
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

/*
        RestClientUtils.setUserAgent(getUserAgent());
*/

       // ABTestingUtils.init();
        SuggestionTable.reset(cmsDB.getDatabase());

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
