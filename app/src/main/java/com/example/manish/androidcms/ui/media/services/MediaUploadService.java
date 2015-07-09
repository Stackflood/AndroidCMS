package com.example.manish.androidcms.ui.media.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.example.manish.androidcms.CMS;

/**
 * Created by Manish on 7/9/2015.
 */
public class MediaUploadService extends Service {

    private Context mContext;
    private boolean mUploadInProgress;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this.getApplicationContext();
        mUploadInProgress = false;

        cancelOldUploads();
    }

    private void cancelOldUploads() {
        // There should be no media files with an upload state of 'uploading' at the start of this service.
        // Since we won't be able to receive notifications for these, set them to 'failed'.

        if (CMS.getCurrentBlog() != null) {
            String blogId = String.valueOf(CMS.getCurrentBlog().getLocalTableBlogId());
            CMS.cmsDB.setMediaUploadingToFailed(blogId);
        }
    }
}
