package com.example.manish.androidcms;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Base64;

import com.example.manish.androidcms.models.Note;
import com.example.manish.androidcms.ui.notifications.NotificationDismissBroadcastReceiver;
import com.example.manish.androidcms.ui.notifications.NotificationsActivity;
import com.example.manish.androidcms.ui.notifications.NotificationsListFragment;
import com.example.manish.androidcms.ui.notifications.utils.NotificationsUtils;
import com.example.manish.androidcms.ui.prefs.AppPrefs;
import com.google.android.gcm.GCMBaseIntentService;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.PhotonUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import Rest.RestRequest;

/**
 * Created by Manish on 9/8/2015.
 */
public class GCMIntentService extends GCMBaseIntentService {

    public static final int PUSH_NOTIFICATION_ID = 1337;
    private static String mPreviousNoteId = null;
    private static long mPreviousNoteTime = 0L;
    private static final int mMaxInboxItems = 5;

    public static final Map<String, Bundle> mActiveNotificationsMap
            = new HashMap<String, Bundle>();


    @Override
    protected String[] getSenderIds(Context context) {
        String[] senderIds = new String[1];
        senderIds[0] = BuildConfig.GCM_ID;
        return senderIds;
    }

    @Override
    protected void onRegistered(Context context, String regId)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        if(!TextUtils.isEmpty(regId))
        {
            String uuid = settings.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_UUID, null);

            if(uuid == null)
            {
                uuid = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(NotificationsUtils.WPCOM_PUSH_DEVICE_UUID, uuid);

            }
        }
    }

    public void refreshNotes(final Context context) {

        NotificationsUtils.refreshNotifications(new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    List<Note> notes = NotificationsUtils.parseNotes(jsonObject);
                    CMS.cmsDB.saveNotes(notes, true);
                    broadcastNewNotification(context);
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.NOTIFS, "Can't parse restRequest JSON response, notifications: " + e);
                }
            }
        }, null);
    }

    protected void handleDefaultPush(Context context, Bundle extras) {
        /*Bundle extras = intent.getExtras();*/
        long wpcomUserID = AppPrefs.getCurrentUserId();
        String userIDFromPN = extras.getString("user");
        if (userIDFromPN != null) { //It is always populated server side,
            // but better to double check it here.
            if (wpcomUserID <= 0) {
                // TODO: Do not abort the execution here, at least for this release, since there might be
                // an issue for users that update the app.
                // If they have never used the Reader, then they won't have a userId.
                // Code for next release is below:
                /* AppLog.e(T.NOTIFS, "No wpcom userId found in the app. Aborting.");
                   return; */
            } else {
                if (!String.valueOf(wpcomUserID).equals(userIDFromPN)) {
                    AppLog.e(AppLog.T.NOTIFS, "wpcom userId found in the app doesn't match with the ID in the PN. Aborting.");
                    return;
                }
            }
        }

        String title = StringEscapeUtils.unescapeHtml(extras.getString("title"));

        if (title == null) {
            title = getString(R.string.app_name);
        }

        String message = StringEscapeUtils.unescapeHtml(extras.getString("msg"));
        String note_id = extras.getString("note_id");

        Note note = null;
        if(extras.getString("note_full_data") != null)
        {
            byte[] decode = Base64.decode(extras.getString("note_full_data"),
                    Base64.DEFAULT);
            String unzippedString = NotificationsUtils.unzipString(decode);
            JSONObject jsonObject = null;
            try
            {
                jsonObject = new JSONObject(unzippedString);
                JSONArray notesJSON = jsonObject.getJSONArray("notes");
                note = new Note(notesJSON.getJSONObject(0));
                CMS.cmsDB.addNote(note,false);
            }
            catch (JSONException e) {
                AppLog.e(AppLog.T.NOTIFS, "Can't parse restRequest JSON response, notifications: ", e);
            }
        }
        else { // create a placeholder note
            note = new Note(extras);
            CMS.cmsDB.addNote(note, true);
            refreshNotes(context);
        }

        /*
         * if this has the same note_id as the previous notification, and the previous notification
         * was received within the last second, then skip showing it - this handles duplicate
         * notifications being shown due to the device being registered multiple times with different tokens.
         * (still investigating how this could happen - 21-Oct-13)
         *
         * this also handles the (rare) case where the user receives rapid-fire sub-second like notifications
         * due to sudden popularity (post gets added to FP and is liked by many people all at once, etc.),
         * which we also want to avoid since it would drain the battery and annoy the user
         *
         * NOTE: different comments on the same post will have a different note_id, but different likes
         * on the same post will have the same note_id, so don't assume that the note_id is unique
         */

        long thisTime = System.currentTimeMillis();
        if (mPreviousNoteId != null && mPreviousNoteId.equals(note_id)) {
            long seconds = TimeUnit.MILLISECONDS.toSeconds(thisTime - mPreviousNoteTime);
            if (seconds <= 1) {
                AppLog.w(AppLog.T.NOTIFS, "skipped potential duplicate notification");
                return;
            }
        }

        mPreviousNoteId = note_id;
        mPreviousNoteTime = thisTime;

        if (note_id != null && !mActiveNotificationsMap.containsKey(note_id)) {
            mActiveNotificationsMap.put(note_id, extras);
        }

        String iconUrl = extras.getString("icon");

        Bitmap largeIconBitmap = null;
        if(iconUrl != null)
        {
            try
            {
                iconUrl = URLDecoder.decode(iconUrl, "UTF-8");

                int largeIconSize = context.getResources().getDimensionPixelSize(
                        android.R.dimen.notification_large_icon_height);
                String resizedUrl = PhotonUtils.getPhotonImageUrl(iconUrl, largeIconSize, largeIconSize);
                largeIconBitmap = ImageUtils.downloadBitmap(resizedUrl);

            }

            catch (UnsupportedEncodingException e) {
                AppLog.e(AppLog.T.NOTIFS, e);
            }
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean sound, vibrate, light;
        sound = prefs.getBoolean("wp_pref_notification_sound", false);
        vibrate = prefs.getBoolean("wp_pref_notification_vibrate", false);
        light = prefs.getBoolean("wp_pref_notification_light", false);

        NotificationCompat.Builder mBuilder;
        Intent resultIntent = new Intent(this, NotificationsActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");

        if(mActiveNotificationsMap.size() <=1)
        {
            mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setColor(getResources().getColor(R.color.blue_wordpress))
                    .setContentTitle(title)
                    .setContentText(message)
                    .setTicker(message)
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

            if(note_id != null)
            {
                resultIntent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, note_id);
            }

            // Add some actions if this is a comment notification
            String noteType = extras.getString("type");
            if(noteType != null && noteType.equals("c")) {
                Intent commentReplyIntent = new Intent(this, NotificationsActivity.class);
                commentReplyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                commentReplyIntent.setAction("android.intent.action.MAIN");
                commentReplyIntent.addCategory("android.intent.category.LAUNCHER");
                commentReplyIntent.addCategory("comment-reply");
                commentReplyIntent.putExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, true);
                if (note_id != null) {
                    commentReplyIntent.putExtra(NotificationsListFragment.NOTE_ID_EXTRA, note_id);
                }

                PendingIntent commentReplyPendingIntent = PendingIntent.getActivity(context,
                        0, commentReplyIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

                mBuilder.addAction(R.drawable.ic_reply_white_24dp, "Reply",
                        commentReplyPendingIntent);
            }
            if (largeIconBitmap != null) {
                mBuilder.setLargeIcon(largeIconBitmap);
            }
        }
        else
        {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            int noteCtr = 1;
            for(Bundle wpPN : mActiveNotificationsMap.values())
            {
                if(noteCtr >  mMaxInboxItems)
                    break;
                if (wpPN.getString("msg") == null)
                    continue;
                if (wpPN.getString("type") != null && wpPN.getString("type").equals("c")) {
                    String pnTitle = StringEscapeUtils.unescapeHtml((wpPN.getString("title")));
                    String pnMessage = StringEscapeUtils.unescapeHtml((wpPN.getString("msg")));
                    inboxStyle.addLine(pnTitle + ": " + pnMessage);
                } else {
                    String pnMessage = StringEscapeUtils.unescapeHtml((wpPN.getString("msg")));
                    inboxStyle.addLine(pnMessage);
                }

                noteCtr++;

            }

            if (mActiveNotificationsMap.size() > mMaxInboxItems) {
                inboxStyle.setSummaryText(String.format(getString(R.string.more_notifications),
                        mActiveNotificationsMap.size() - mMaxInboxItems));
            }

            String subject = String.format(getString(R.string.new_notifications), mActiveNotificationsMap.size());

            mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setColor(getResources().getColor(R.color.blue_wordpress))
                    .setContentTitle("WordPress")
                    .setTicker(message)
                    .setAutoCancel(true)
                    .setStyle(inboxStyle);
        }
        // Call broadcast receiver when notification is dismissed

        Intent notificationDeletedIntent = new Intent(this, NotificationDismissBroadcastReceiver.class);
        PendingIntent pendingDeleteIntent = PendingIntent.getBroadcast(
                context, 0, notificationDeletedIntent, 0
        );

        mBuilder.setDeleteIntent(pendingDeleteIntent);
        if (sound) {
            mBuilder.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.notification));
        }
        if (vibrate) {
            mBuilder.setVibrate(new long[]{500, 500, 500});
        }
        if (light) {
            mBuilder.setLights(0xff0000ff, 1000, 5000);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context,0, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(PUSH_NOTIFICATION_ID, mBuilder.build());
        broadcastNewNotification(context);

    }

    public void broadcastNewNotification(Context context) {
        Intent msgIntent = new Intent();
        msgIntent.setAction(NotificationsActivity.NOTIFICATION_ACTION);
        LocalBroadcastManager.getInstance(context).sendBroadcast(msgIntent);
    }

    @Override
    protected void onMessage(Context context, Intent intent) {

        AppLog.v(AppLog.T.NOTIFS, "Received Message");
        Bundle extras = intent.getExtras();
        if (extras == null) {
            AppLog.v(AppLog.T.NOTIFS, "No notification message content received. Aborting.");
            return;
        }

        if (!CMS.hasDotComToken(context)) {
            return;
        }

        handleDefaultPush(context, extras);




    }

    @Override
    protected void onError(Context context, String errorId)
    {
        AppLog.e(AppLog.T.NOTIFS, "GCM Error: " + errorId);
    }

    @Override
    protected void onUnregistered(Context context, String regId)
    {

    }

    public static void clearNotificationsMap() {
        mActiveNotificationsMap.clear();
    }
}
