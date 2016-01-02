package com.example.manish.androidcms.ui.reader.actions;

import com.android.volley.VolleyError;
import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.datasets.ReaderUserTable;
import com.example.manish.androidcms.models.ReaderUser;
import com.example.manish.androidcms.ui.prefs.AppPrefs;

import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import Rest.RestRequest;

/**
 * Created by Manish on 4/15/2015.
 */
public class ReaderUserActions {


    /*
     * request the current user's info, update locally if different than existing local
     */
    public static void updateCurrentUser() {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
        public void onResponse(JSONObject jsonObject)
            {
                final ReaderUser serverUser = ReaderUser.fromJson(jsonObject);
                final ReaderUser localUser = ReaderUserTable.getCurrentUser();

                if (serverUser != null && !serverUser.isSameUser(localUser)) {
                    setCurrentUser(serverUser);
                }
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.READER, volleyError);
            }
        };

        CMS.getRestClientUtilsV1_1().get("me", listener, errorListener);

    }


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
