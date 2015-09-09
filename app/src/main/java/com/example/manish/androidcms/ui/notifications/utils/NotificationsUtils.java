package com.example.manish.androidcms.ui.notifications.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.android.volley.VolleyError;
import com.example.manish.androidcms.BuildConfig;
import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.models.Note;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DeviceUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.InflaterInputStream;

import Rest.RestRequest;

/**
 * Created by Manish on 9/8/2015.
 */
public class NotificationsUtils {


    public static final String WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS = "wp_pref_notification_settings";
    private static final String WPCOM_PUSH_DEVICE_SERVER_ID = "wp_pref_notifications_server_id";
    public static final String WPCOM_PUSH_DEVICE_UUID = "wp_pref_notifications_uuid";


    public static void refreshNotifications(final RestRequest.Listener listener,
                                            final RestRequest.ErrorListener errorListener) {
        CMS.getRestClientUtils().getNotifications(new RestRequest.Listener() {
                                                            @Override
                                                            public void onResponse(JSONObject response) {
                                                                if (listener != null) {
                                                                    listener.onResponse(response);
                                                                }
                                                            }
                                                        }, new RestRequest.ErrorListener() {
                                                            @Override
                                                            public void onErrorResponse(VolleyError error) {
                                                                if (errorListener != null) {
                                                                    errorListener.onErrorResponse(error);
                                                                }
                                                            }
                                                        }
        );
    }

    public static List<Note> parseNotes(JSONObject response) throws JSONException {
        List<Note> notes;
        JSONArray notesJSON = response.getJSONArray("notes");
        notes = new ArrayList<Note>(notesJSON.length());
        for (int i = 0; i < notesJSON.length(); i++) {
            Note n = new Note(notesJSON.getJSONObject(i));
            notes.add(n);
        }
        return notes;
    }

    private static String getAppPushNotificationsName() {
        //white listing only few keys.
        if (BuildConfig.APP_PN_KEY.equals("org.wordpress.android.beta.build"))
            return "org.wordpress.android.beta.build";
        if (BuildConfig.APP_PN_KEY.equals("org.wordpress.android.debug.build"))
            return "org.wordpress.android.debug.build";

        return "org.wordpress.android.playstore";
    }

    public static void registerDeviceForPushNotifications(final Context ctx, String token) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);

        String uuid = settings.getString(WPCOM_PUSH_DEVICE_UUID, null);
        if(uuid == null)
            return;

        String deviceName = DeviceUtils.getInstance().getDeviceName(ctx);
        final Map<String, String> contentStruct = new HashMap<>();

        contentStruct.put("device_token", token);
        contentStruct.put("device_family", "android");
        contentStruct.put("app_secret_key", NotificationsUtils.getAppPushNotificationsName());
        contentStruct.put("device_name", deviceName);
        contentStruct.put("device_model",  Build.MANUFACTURER + " " + Build.MODEL);
        contentStruct.put("app_version", CMS.versionName);
        contentStruct.put("os_version",  Build.VERSION.RELEASE);
        contentStruct.put("device_uuid", uuid);

        RestRequest.Listener listener = new RestRequest.Listener()
        {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.d(AppLog.T.NOTIFS, "Register token action succeeded");
                try {
                    String deviceID = jsonObject.getString("ID");
                    if (deviceID == null) {
                        AppLog.e(AppLog.T.NOTIFS, "Server response is missing of the device_id." +
                                " Registration skipped!!");
                        return;
                    }

                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(WPCOM_PUSH_DEVICE_SERVER_ID, deviceID);
                    JSONObject settingsJSON = jsonObject.getJSONObject("settings");
                    editor.putString(WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, settingsJSON.toString());
                    editor.apply();
                    AppLog.d(AppLog.T.NOTIFS, "Server response OK. The device_id : " + deviceID);


                } catch (JSONException e1) {
                    AppLog.e(AppLog.T.NOTIFS, "Server response is NOT ok. Registration skipped!!", e1);
                }




            }

        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.NOTIFS, "Register token action failed", volleyError);
            }
        };

        CMS.getRestClientUtils().post("/devices/new",
                contentStruct, null, listener, errorListener);
    }

    public static String unzipString(byte[] zbytes) {
        try {
            // Add extra byte to array when Inflater is set to true
            byte[] input = new byte[zbytes.length + 1];
            System.arraycopy(zbytes, 0, input, 0, zbytes.length);
            input[zbytes.length] = 0;
            ByteArrayInputStream bin = new ByteArrayInputStream(input);
            InflaterInputStream in = new InflaterInputStream(bin);
            ByteArrayOutputStream bout = new ByteArrayOutputStream(512);
            int b;
            while ((b = in.read()) != -1) {
                bout.write(b);
            }
            bout.close();
            return bout.toString();
        } catch (IOException io) {
            AppLog.e(AppLog.T.NOTIFS, "Unzipping failed", io);
            return null;
        }
    }
}
