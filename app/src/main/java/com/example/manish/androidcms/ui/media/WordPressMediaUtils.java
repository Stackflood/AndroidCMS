package com.example.manish.androidcms.ui.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;

import org.wordpress.android.util.AppLog;
import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;

import static org.wordpress.mediapicker.MediaUtils.fadeInImage;

/**
 * Created by Manish on 6/9/2015.
 */
public class WordPressMediaUtils {

    public class RequestCode {
        public static final int ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY = 1000;
        public static final int ACTIVITY_REQUEST_CODE_TAKE_PHOTO = 1100;
        public static final int ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY = 1200;
        public static final int ACTIVITY_REQUEST_CODE_TAKE_VIDEO = 1300;
    }

    public static Cursor getWordPressMediaImages(String blogId) {
        return CMS.cmsDB.getMediaImagesForBlog(blogId);
    }

    public static Cursor getWordPressMediaVideos(String blogId) {
        return CMS.cmsDB.getMediaFilesForBlog(blogId);
    }
    public interface LaunchCameraCallback {
        public void onMediaCapturePathReady(String mediaCapturePath);
    }


    public static void launchVideoCamera(Activity activity) {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        activity.startActivityForResult(intent, RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    public static class BackgroundDownloadWebImage extends AsyncTask<Uri, String, Bitmap> {
        WeakReference<ImageView> mReference;

        public BackgroundDownloadWebImage(ImageView resultStore) {
            mReference = new WeakReference<>(resultStore);
        }

        @Override
        protected Bitmap doInBackground(Uri... params) {
            try {
                String uri = params[0].toString();
                Bitmap bitmap = CMS.getBitmapCache().getBitmap(uri);

                if (bitmap == null) {
                    URL url = new URL(uri);
                    bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    CMS.getBitmapCache().put(uri, bitmap);
                }

                return bitmap;
            }
            catch(IOException notFoundException) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            ImageView imageView = mReference.get();

            if (imageView != null) {
                if (imageView.getTag() == this) {
                    imageView.setImageBitmap(result);
                    fadeInImage(imageView, result);
                }
            }
        }
    }




    public static void launchCamera(Activity activity,
                                    LaunchCameraCallback callback)
    {
        String state = android.os.Environment.getExternalStorageState();
        if(!state.equals(Environment.MEDIA_MOUNTED))
        {
            showSDCardRequiredDialog(activity);
        }
        else
        {
            Intent intent = prepareLaunchCameraIntent(callback);
            activity.startActivityForResult(intent, RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO);
            AppLockManager.getInstance().setExtendedTimeout();
        }
    }

    private static Intent prepareLaunchCameraIntent(LaunchCameraCallback callback) {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        String mediaCapturePath = path + File.separator + "Camera" + File.separator +
                "wp-" + System.currentTimeMillis() + ".jpg";

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(new File(mediaCapturePath)));

        if (callback != null) {
            callback.onMediaCapturePathReady(mediaCapturePath);
        }

        // make sure the directory we plan to store the recording in exists
        File directory = new File(mediaCapturePath).getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            try {
                throw new IOException("Path to file could not be created.");
            } catch (IOException e) {
                AppLog.e(AppLog.T.POSTS, e);
            }
        }
        return intent;
    }

    private static void showSDCardRequiredDialog(Activity activity)
    {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(activity.getResources().getText(R.string.sdcard_title));
        dialogBuilder.setMessage(activity.getResources().getText(R.string.sdcard_message));
        dialogBuilder.setPositiveButton(activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }
}
