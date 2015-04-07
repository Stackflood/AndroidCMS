package com.example.manish.androidcms.ui.prefs;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.ui.ActivityId;

/**
 * Created by Manish on 4/1/2015.
 */
public class AppPrefs {

    public enum PrefKey {
        // id of the current user
        USER_ID,

        // name of last shown activity
        LAST_ACTIVITY_STR,

        // last selected tag in the reader
        READER_TAG_NAME,
        READER_TAG_TYPE,

        // title of the last active page in ReaderSubsActivity
        READER_SUBS_PAGE_TITLE,

        // email retrieved and attached to mixpanel profile
        MIXPANEL_EMAIL_ADDRESS,
    }

    private static SharedPreferences prefs()
    {
        return PreferenceManager.getDefaultSharedPreferences(CMS.getContext());

    }
    public static void setLastActivityStr(String value) {
        setString(PrefKey.LAST_ACTIVITY_STR, value);
    }

    private static void setString(PrefKey key, String value) {
        SharedPreferences.Editor editor = prefs().edit();
        if (TextUtils.isEmpty(value)) {
            editor.remove(key.name());
        } else {
            editor.putString(key.name(), value);
        }
        editor.apply();
    }

    private static String getString(PrefKey key) {
        return getString(key, "");
    }

    private static String getString(PrefKey key, String defaultValue) {
        return prefs().getString(key.name(), defaultValue);
    }
    /**
     * name of the last shown activity - used at startup to restore the previously selected
     * activity, also used by analytics tracker
     */
    public static String getLastActivityStr() {
        return getString(PrefKey.LAST_ACTIVITY_STR, ActivityId.UNKNOWN.name());
    }

}
