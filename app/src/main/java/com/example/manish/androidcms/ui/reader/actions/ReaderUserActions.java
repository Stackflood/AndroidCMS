package com.example.manish.androidcms.ui.reader.actions;

import com.example.manish.androidcms.datasets.ReaderUserTable;
import com.example.manish.androidcms.models.ReaderUser;
import com.example.manish.androidcms.ui.prefs.AppPrefs;

import org.json.JSONObject;

/**
 * Created by Manish on 4/15/2015.
 */
public class ReaderUserActions {

    /*
     * set the passed user as the current user in both the local db and prefs
     */
    public static void setCurrentUser(JSONObject jsonUser) {
        if (jsonUser == null)
            return;
        setCurrentUser(ReaderUser.fromJson(jsonUser));
    }
    private static void setCurrentUser(ReaderUser user) {
        if (user == null)
            return;
        ReaderUserTable.addOrUpdateUser(user);
        AppPrefs.setCurrentUserId(user.userId);
    }
}
