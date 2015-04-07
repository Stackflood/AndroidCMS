package com.example.manish.androidcms.ui;

import com.example.manish.androidcms.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;

/**
 * Created by Manish on 4/1/2015.
 */
public enum ActivityId {
    UNKNOWN("Unknown"),
    READER("Reader"),
    NOTIFICATIONS("Notifications"),
    POSTS("Post List"),
    MEDIA("Media Library"),
    PAGES("Page List"),
    COMMENTS("Comments"),
    THEMES("Themes"),
    STATS("Stats"),
    VIEW_SITE("View Site"),
    POST_EDITOR("Post Editor"),
    LOGIN("Login Screen");

    private final String mStringValue;

    public static void trackLastActivity(ActivityId activityId) {
        AppLog.v(AppLog.T.UTILS, "trackLastActivity, activityId: " + activityId);
        if (activityId != null) {
            AppPrefs.setLastActivityStr(activityId.name());
        }
    }

    public String toString() {
        return mStringValue;
    }

    private ActivityId(final String stringValue) {
        mStringValue = stringValue;
    }

    public static ActivityId getActivityIdFromName(String activityString) {
        if (activityString == null) {
            return ActivityId.UNKNOWN;
        }
        try {
            return ActivityId.valueOf(activityString);
        } catch (IllegalArgumentException e) {
            // default to UNKNOWN in case the activityString is bogus
            return ActivityId.UNKNOWN;
        }
    }

}
