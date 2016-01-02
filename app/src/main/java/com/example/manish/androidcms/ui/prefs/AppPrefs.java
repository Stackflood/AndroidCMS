package com.example.manish.androidcms.ui.prefs;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.models.ReaderTag;
import com.example.manish.androidcms.models.ReaderTagType;
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

    private static int getInt(PrefKey key) {
        try {
            String value = getString(key);
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void setInt(PrefKey key, int value) {
        setString(key, Integer.toString(value));
    }

    public static void setReaderTag(ReaderTag tag)
    {
        if(tag!=null && !TextUtils.isEmpty(tag.getTagName()))
        {
            setString(PrefKey.READER_TAG_NAME, tag.getTagName());
            setInt(PrefKey.READER_TAG_TYPE, tag.tagType.toInt());
        }
        else
        {
            prefs().edit().remove(PrefKey.READER_TAG_NAME.name()).
                    remove(PrefKey.READER_TAG_TYPE.name()).
                    apply();
        }
    }

    public static ReaderTag getReaderTag() {
        String tagName = getString(PrefKey.READER_TAG_NAME);
        if (TextUtils.isEmpty(tagName)) {
            return null;
        }
        int tagType = getInt(PrefKey.READER_TAG_TYPE);
        return new ReaderTag(tagName, ReaderTagType.fromInt(tagType));
    }

    /**
     * remove all user-related preferences
     */
    public static void reset() {
        SharedPreferences.Editor editor = prefs().edit();
        for (PrefKey key : PrefKey.values()) {
            editor.remove(key.name());
        }
        editor.apply();
    }
    private static long getLong(PrefKey key) {
        try {
            String value = getString(key);
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void setLong(PrefKey key, long value) {
        setString(key, Long.toString(value));
    }

    private static void remove(PrefKey key) {
        prefs().edit().remove(key.name()).apply();
    }

    public static long getCurrentUserId() {
        return getLong(PrefKey.USER_ID);
    }
    public static void setCurrentUserId(long userId) {
        if (userId == 0) {
            remove(PrefKey.USER_ID);
        } else {
            setLong(PrefKey.USER_ID, userId);
        }
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
