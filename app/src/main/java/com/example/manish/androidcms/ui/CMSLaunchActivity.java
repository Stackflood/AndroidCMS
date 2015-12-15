package com.example.manish.androidcms.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.ui.posts.PostsActivity;
import com.example.manish.androidcms.ui.prefs.AppPrefs;
import com.example.manish.androidcms.ui.prefs.SettingsFragment;

import com.example.manish.androidcms.util.CMSActivityUtils;


import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;

import java.util.Locale;

/**
 * Created by Manish on 4/1/2015.
 */
public class CMSLaunchActivity extends ActionBarActivity {

    /*
     * this the main (default) activity, which does nothing more than launch the
     * previously active activity on startup
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyLocale();
        setContentView(R.layout.activity_launch);

        if(CMS.cmsDB == null)
        {
            ToastUtils.showToast(this, R.string.fatal_db_error, ToastUtils.Duration.LONG);
            finish();
            return;
        }

        String lastActivityStr = AppPrefs.getLastActivityStr();
        ActivityId id = ActivityId.getActivityIdFromName(lastActivityStr);

        Intent intent = CMSActivityUtils.getIntentForActivityId(this, id);
        AppLog.v(AppLog.T.UTILS, "WPLaunchActivity,  activityName: " + lastActivityStr + ", activityId: " + id + ", " +
                "intent: " + intent);

        if(intent == null)
        {
            AppLog.v(AppLog.T.UTILS, "Launch default Activity: PostsActivity");
            intent = new Intent(this, PostsActivity.class);
        }

        //added this so application shouldn't break the next time it starts
        intent = new Intent(this, PostsActivity.class);
        startActivity(intent);
        finish();
    }

    private void applyLocale() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.contains(SettingsFragment.SETTINGS_PREFERENCES)) {

            String locale = sharedPreferences.getString(SettingsFragment.SETTINGS_PREFERENCES, "");

            if (!locale.equals(Locale.getDefault().getDisplayLanguage())) {
                Resources resources = getResources();
                Configuration conf = resources.getConfiguration();
                conf.locale = new Locale(locale);
                resources.updateConfiguration(conf, resources.getDisplayMetrics());

                Intent refresh = new Intent(this,CMSLaunchActivity.class);
                startActivity(refresh);
                finish();
            }
        }
    }

}
