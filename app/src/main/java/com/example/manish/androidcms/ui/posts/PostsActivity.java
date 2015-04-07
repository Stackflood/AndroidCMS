package com.example.manish.androidcms.ui.posts;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.Post;
import com.example.manish.androidcms.ui.CMSDrawerActivity;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ProfilingUtils;
import com.example.manish.androidcms.widgets.CMSAlertDialogFragment;

/**
 * Created by Manish on 4/1/2015.
 */
public class PostsActivity extends CMSDrawerActivity
        implements PostsListFragment.OnPostSelectedListener,
        PostsListFragment.OnSinglePostLoadedListener,
        PostsListFragment.OnPostActionListener,
        ViewPostFragment.OnDetailPostActionListener,
        CMSAlertDialogFragment.OnDialogConfirmListener
{

    public static final String EXTRA_VIEW_PAGES = "viewPages";
    public static final String EXTRA_ERROR_MSG = "errorMessage";
    public static final String EXTRA_ERROR_INFO_TITLE = "errorInfoTitle";
    public static final String EXTRA_ERROR_INFO_LINK = "errorInfoLink";

    public static final int POST_DELETE = 0, POST_SHARE = 1, POST_EDIT = 2, POST_CLEAR = 3, POST_VIEW = 5;
    public static final int ACTIVITY_EDIT_POST = 0;
    private static final int ID_DIALOG_DELETING = 1, ID_DIALOG_SHARE = 2;
    public ProgressDialog mLoadingDialog;

    public boolean mIsPage = false;
    public String mErrorMsg = "";
    private PostsListFragment mPostList;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ProfilingUtils.split("PostsActivity.onCreate");
        ProfilingUtils.dump();

        createMenuDrawer(R.layout.posts);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
        }

        FragmentManager fm = getFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);

        mPostList = (PostsListFragment) fm.findFragmentById(R.id.postList);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mIsPage = extras.getBoolean(EXTRA_VIEW_PAGES);
            showErrorDialogIfNeeded(extras);
        }

        if (mIsPage) {
            getSupportActionBar().setTitle(getString(R.string.pages));
        } else {
            getSupportActionBar().setTitle(getString(R.string.posts));
        }

        CMS.currentPost = null;

        if (savedInstanceState != null) {
          //  popPostDetail();
        }

       //attemptToSelectPost();
        //finish();
    }

    protected void attemptToSelectPost() {
        FragmentManager fm = getFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm.findFragmentById(R.id.postDetail);
        if (f != null && f.isInLayout()) {
            mPostList.setShouldSelectFirstPost(true);
        }
    }

    /*Receive the result from a previous call to startActivityForResult(Intent, int).*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {

    }
    protected void popPostDetail() {
        if (isFinishing()) {
            return;
        }

        FragmentManager fm = getFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm.findFragmentById(R.id.postDetail);
        if (f == null) {
            try {
                fm.popBackStack();
            } catch (RuntimeException e) {
                AppLog.e(AppLog.T.POSTS, e);
            }
        }
    }

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getDrawerToggle() != null) {
                getDrawerToggle().setDrawerIndicatorEnabled(getFragmentManager().getBackStackEntryCount() == 0);
            }
        }
    };

    private void showErrorDialogIfNeeded(Bundle extras) {
        if (extras == null) {
            return;
        }
        String errorMessage = extras.getString(EXTRA_ERROR_MSG);
        if (!TextUtils.isEmpty(errorMessage)) {
            String errorInfoTitle = extras.getString(EXTRA_ERROR_INFO_TITLE);
            String errorInfoLink = extras.getString(EXTRA_ERROR_INFO_LINK);
            showPostUploadErrorAlert(errorMessage, errorInfoTitle, errorInfoLink);
        }
    }

    private void showPostUploadErrorAlert(String errorMessage, String infoTitle,
                                          final String infoURL) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PostsActivity.this);
        dialogBuilder.setTitle(getResources().getText(R.string.error));
        dialogBuilder.setMessage(errorMessage);
        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Just close the window.
                    }
                }
        );
        if (infoTitle != null && infoURL != null) {
            dialogBuilder.setNeutralButton(infoTitle,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(infoURL)));
                        }
                    });
        }
        dialogBuilder.setCancelable(true);
        if (!isFinishing())
            dialogBuilder.create().show();
    }
    @Override
    public void onPostSelected(Post post) {

    }

    @Override
    public void onDetailPostAction(int action, Post post) {

    }

    @Override
    public void onDialogConfirm() {

    }

    @Override
    public void onSinglePostLoaded() {

    }

    @Override
    public void onPostAction(int action, Post post) {

    }
}
