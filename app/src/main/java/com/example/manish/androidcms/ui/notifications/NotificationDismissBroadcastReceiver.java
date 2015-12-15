package com.example.manish.androidcms.ui.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.manish.androidcms.GCMIntentService;

/**
 * Created by Manish on 9/8/2015.
 */
public class NotificationDismissBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        GCMIntentService.clearNotificationsMap();
    }
}
