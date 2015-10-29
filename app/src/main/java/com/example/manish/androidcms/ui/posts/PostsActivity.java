package com.example.manish.androidcms.ui.posts;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.widget.Toast;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.Blog;
import com.example.manish.androidcms.models.Post;
import com.example.manish.androidcms.models.PostStatus;
import com.example.manish.androidcms.ui.CMSDrawerActivity;

import org.wordpress.android.util.AlertUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.passcodelock.AppLockManager;
import org.xmlpull.v1.XmlPullParserException;

import com.example.manish.androidcms.util.WPMeShortlinks;
import com.example.manish.androidcms.widgets.CMSAlertDialogFragment;

import java.io.IOException;

import xmlrpc.android.ApiHelper;
import xmlrpc.android.XMLRPCClientInterface;
import xmlrpc.android.XMLRPCException;
import xmlrpc.android.XMLRPCFactory;

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

    public static final int POST_DELETE = 0,
            POST_SHARE = 1, POST_EDIT = 2, POST_CLEAR = 3, POST_VIEW = 5;
    public static final int ACTIVITY_EDIT_POST = 0;
    private static final int ID_DIALOG_DELETING = 1, ID_DIALOG_SHARE = 2;
    public ProgressDialog mLoadingDialog;

    public boolean mIsPage = false;
    public String mErrorMsg = "";
    private PostsListFragment mPostList;

    @Override
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
            popPostDetail();
        }

       attemptToSelectPost();

    }


    public class refreshCommentsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Object[] commentParams = { CMS.currentBlog.getRemoteBlogId(),
                    CMS.currentBlog.getUsername(),
                    CMS.currentBlog.getPassword() };

            try {
                ApiHelper.refreshComments(CMS.currentBlog, commentParams);
            } catch (final Exception e) {
                mErrorMsg = getResources().getText(R.string.error_generic).toString();
            }
            return null;
        }
    }
    protected void refreshComments() {
        new refreshCommentsTask().execute();
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

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getDrawerToggle() != null) {
                getDrawerToggle().setDrawerIndicatorEnabled(getFragmentManager().getBackStackEntryCount() == 0);
            }
        }
    };

    public boolean isRefreshing() {
        return mPostList.isRefreshing();
    }

    public void requestPosts() {
        if (CMS.getCurrentBlog() == null) {
            return;
        }
        // If user has local changes, don't refresh
        if (!CMS.cmsDB.findLocalChanges(CMS.getCurrentBlog().getLocalTableBlogId(), mIsPage)) {
            popPostDetail();
            mPostList.requestPosts(false);
            mPostList.setRefreshing(true);
        }
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            popPostDetail();
        } else {
            super.onBackPressed();
        }
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

    @Override
    protected void onResume() {
        super.onResume();

        // posts can't be shown if there aren't any visible blogs,
        // so redirect to the reader and
        // exit the post list in this situation
        if (CMS.isSignedIn(PostsActivity.this)) {
            if (showCorrectActivityForAccountIfRequired()) {
                finish();
            }
        }

        if (CMS.postsShouldRefresh) {
            requestPosts();
            mPostList.setRefreshing(true);
            CMS.postsShouldRefresh = false;
        }
    }

    public void newPost() {
        if (CMS.getCurrentBlog() == null) {
            if (!isFinishing())
                Toast.makeText(this, R.string.blog_not_found,
                        Toast.LENGTH_SHORT).show();
            return;
        }
        // Create a new post object
        Post newPost = new Post(CMS.getCurrentBlog().getLocalTableBlogId(), mIsPage);
        CMS.cmsDB.savePost(newPost);
        Intent i = new Intent(this, EditPostActivity.class);
        i.putExtra(EditPostActivity.EXTRA_POSTID, newPost.getLocalTablePostId());
        i.putExtra(EditPostActivity.EXTRA_IS_PAGE, mIsPage);
        i.putExtra(EditPostActivity.EXTRA_IS_NEW_POST, true);
        startActivityForResult(i, ACTIVITY_EDIT_POST);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /*Receive the result from a previous call to startActivityForResult(Intent, int).*/


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (data != null) {
            if (requestCode == ACTIVITY_EDIT_POST && resultCode == RESULT_OK) {
                if (data.getBooleanExtra("shouldRefresh", false)) {
                    mPostList.getPostListAdapter().loadPosts();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void attemptToSelectPost() {
        FragmentManager fm = getFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm.findFragmentById(R.id.postDetail);
        if (f != null && f.isInLayout()) {
            mPostList.setShouldSelectFirstPost(true);
        }
    }

    @Override
    public void onPostSelected(Post post) {

        if (isFinishing() || isActivityDestroyed()) {
            return;
        }

        FragmentManager fm = getFragmentManager();
        ViewPostFragment viewPostFragment = (ViewPostFragment)
                fm.findFragmentById(R.id.postDetail);

        if (post != null) {
            if (post.isUploading()){
                ToastUtils.showToast(this, R.string.toast_err_post_uploading,
                        ToastUtils.Duration.SHORT);
                return;
            }
            CMS.currentPost = post;
            if (viewPostFragment == null || !viewPostFragment.isInLayout()) {
                FragmentTransaction ft = fm.beginTransaction();
                ft.hide(mPostList);
                viewPostFragment = new ViewPostFragment();
                ft.add(R.id.postDetailFragmentContainer, viewPostFragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
            } else {
                viewPostFragment.loadPost(post);
            }
        }

    }

    @Override
    public void onPostAction(int action, final Post post) {
        // No post? No service.
        if (post == null) {
            Toast.makeText(PostsActivity.this,
                    R.string.post_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        if(action == POST_DELETE)
        {
            if(post.isLocalDraft())
            {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PostsActivity.this);
                dialogBuilder.setTitle(getResources().getText(
                        R.string.delete_draft));
                String deleteDraftMessage = getResources().getText(R.string.delete_sure).toString();
                if (!post.getTitle().isEmpty()) {
                    String postTitleEnclosedByQuotes = "'" + post.getTitle() + "'";
                    deleteDraftMessage += " " + postTitleEnclosedByQuotes;
                }

                dialogBuilder.setMessage(deleteDraftMessage + "?");
                dialogBuilder.setPositiveButton(
                        getResources().getText(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                CMS.cmsDB.deletePost(post);
                                popPostDetail();
                                attemptToSelectPost();
                                mPostList.getPostListAdapter().loadPosts();
                            }
                        });
                dialogBuilder.setNegativeButton(
                        getResources().getText(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // Just close the window.
                            }
                        });
                dialogBuilder.setCancelable(true);
                if (!isFinishing()) {
                    dialogBuilder.create().show();
                }
            }
            else {
                String deletePostMessage = getResources().getText(
                        (post.isPage()) ? R.string.delete_sure_page
                                : R.string.delete_sure_post).toString();
                if (!post.getTitle().isEmpty()) {
                    String postTitleEnclosedByQuotes = "'" + post.getTitle() + "'";
                    deletePostMessage += " " + postTitleEnclosedByQuotes;
                }

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        PostsActivity.this);
                dialogBuilder.setTitle(getResources().getText(
                        (post.isPage()) ? R.string.delete_page
                                : R.string.delete_post));
                dialogBuilder.setMessage(deletePostMessage + "?");
                dialogBuilder.setPositiveButton(
                        getResources().getText(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                new deletePostTask().execute(post);
                            }
                        });
                dialogBuilder.setNegativeButton(
                        getResources().getText(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // Just close the window.
                            }
                        });
                dialogBuilder.setCancelable(true);
                if (!isFinishing()) {
                    dialogBuilder.create().show();
                }
            }
        }
        else if(action == POST_SHARE) {
            // Only share published posts
            if (post.getStatusEnum() != PostStatus.PUBLISHED &&
                    post.getStatusEnum() != PostStatus.SCHEDULED) {

                AlertUtils.showAlert(this, R.string.error,
                        post.isPage() ? R.string.page_not_published : R.string.post_not_published);
                return;
            }

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, post.getTitle());
            String shortlink = WPMeShortlinks.getPostShortlink(CMS.getCurrentBlog(), post);
            share.putExtra(Intent.EXTRA_TEXT,
                    shortlink != null ? shortlink : post.getPermaLink());
            startActivity(Intent.createChooser(share, getResources()
                    .getText(R.string.share_url)));
            AppLockManager.getInstance().setExtendedTimeout();
        }
        else if (action == POST_CLEAR) {
            FragmentManager fm = getFragmentManager();
            ViewPostFragment f = (ViewPostFragment) fm
                    .findFragmentById(R.id.postDetail);
            if (f != null) {
                f.clearContent();
            }
        }

        /*if (action == POST_DELETE) {
            if (post.isLocalDraft()) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        PostsActivity.this);
                dialogBuilder.setTitle(getResources().getText(
                        R.string.delete_draft));

                String deleteDraftMessage = getResources().getText(R.string.delete_sure).toString();
                if (!post.getTitle().isEmpty()) {
                    String postTitleEnclosedByQuotes = "'" + post.getTitle() + "'";
                    deleteDraftMessage += " " + postTitleEnclosedByQuotes;
                }

                dialogBuilder.setMessage(deleteDraftMessage + "?");
                dialogBuilder.setPositiveButton(
                        getResources().getText(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                CMS.cmsDB.deletePost(post);
                                popPostDetail();
                                attemptToSelectPost();
                                mPostList.getPostListAdapter().loadPosts();
                            }
                        });
                dialogBuilder.setNegativeButton(
                        getResources().getText(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // Just close the window.
                            }
                        });
                dialogBuilder.setCancelable(true);
                if (!isFinishing()) {
                    dialogBuilder.create().show();
                }
            } else {
                String deletePostMessage = getResources().getText(
                        (post.isPage()) ? R.string.delete_sure_page
                                : R.string.delete_sure_post).toString();
                if (!post.getTitle().isEmpty()) {
                    String postTitleEnclosedByQuotes = "'" + post.getTitle() + "'";
                    deletePostMessage += " " + postTitleEnclosedByQuotes;
                }

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        PostsActivity.this);
                dialogBuilder.setTitle(getResources().getText(
                        (post.isPage()) ? R.string.delete_page
                                : R.string.delete_post));
                dialogBuilder.setMessage(deletePostMessage + "?");
                dialogBuilder.setPositiveButton(
                        getResources().getText(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                new deletePostTask().execute(post);
                            }
                        });
                dialogBuilder.setNegativeButton(
                        getResources().getText(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // Just close the window.
                            }
                        });
                dialogBuilder.setCancelable(true);
                if (!isFinishing()) {
                    dialogBuilder.create().show();
                }
            }
        } else if (action == POST_SHARE) {
            // Only share published posts
            if (post.getStatusEnum() != PostStatus.PUBLISHED && post.getStatusEnum() != PostStatus.SCHEDULED) {
                AlertUtils.showAlert(this, R.string.error,
                        post.isPage() ? R.string.page_not_published : R.string.post_not_published);
                return;
            }

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, post.getTitle());
            String shortlink = WPMeShortlinks.getPostShortlink(WordPress.getCurrentBlog(), post);
            share.putExtra(Intent.EXTRA_TEXT, shortlink != null ? shortlink : post.getPermaLink());
            startActivity(Intent.createChooser(share, getResources()
                    .getText(R.string.share_url)));
            AppLockManager.getInstance().setExtendedTimeout();
        } else if (action == POST_CLEAR) {
            FragmentManager fm = getFragmentManager();
            ViewPostFragment f = (ViewPostFragment) fm
                    .findFragmentById(R.id.postDetail);
            if (f != null) {
                f.clearContent();
            }
        }*/
    }

    public class deletePostTask extends AsyncTask<Post, Void, Boolean>
    {
        Post post;

        @Override
        protected void onPreExecute()
        {
            // pop out of the detail view if on a smaller screen
            popPostDetail();
            showDialog(ID_DIALOG_DELETING);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                CMS.cmsDB.deletePost(post);
            }
            if (mLoadingDialog == null || isActivityDestroyed() || isFinishing()) {
                return;
            }
            dismissDialog(ID_DIALOG_DELETING);
            attemptToSelectPost();
            if (result) {
                Toast.makeText(PostsActivity.this, getResources().getText((mIsPage) ?
                                R.string.page_deleted : R.string.post_deleted),
                        Toast.LENGTH_SHORT).show();
                requestPosts();
                mPostList.requestPosts(false);
                mPostList.setRefreshing(true);
            } else {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PostsActivity.this);
                dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
                dialogBuilder.setMessage(mErrorMsg);
                dialogBuilder.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Just close the window.
                            }
                        });
                dialogBuilder.setCancelable(true);
                if (!isFinishing()) {
                    dialogBuilder.create().show();
                }
            }
        }

        @Override
        protected Boolean doInBackground(Post... params) {
            boolean result = false;
            post = params[0];
            Blog blog = CMS.currentBlog;
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(),

                    blog.getHttpuser(),
                    blog.getHttppassword());

            Object[] postParams = { "", post.getRemotePostId(),
                    CMS.currentBlog.getUsername(),
                    CMS.currentBlog.getPassword() };
            Object[] pageParams = { CMS.currentBlog.getRemoteBlogId(),
                    CMS.currentBlog.getUsername(),
                    CMS.currentBlog.getPassword(), post.getRemotePostId() };

            try {
                client.call((mIsPage) ? "wp.deletePage" :
                        "blogger.deletePost", (mIsPage) ? pageParams : postParams);
                result = true;
            } catch (final XMLRPCException e) {
                mErrorMsg = prepareErrorMessage(e);
            } catch (IOException e) {
                mErrorMsg = prepareErrorMessage(e);
            } catch (XmlPullParserException e) {
                mErrorMsg = prepareErrorMessage(e);
            }
            return result;
        }

        private String prepareErrorMessage(Exception e) {
            AppLog.e(AppLog.T.POSTS, "Error while deleting post or page", e);
            return String.format(getResources().getString(R.string.error_delete_post),
                    (mIsPage) ? getResources().getText(R.string.page)
                            : getResources().getText(R.string.post));
        }

    }

    @Override
    public void onDetailPostAction(int action, Post post) {

        onPostAction(action, post);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        mLoadingDialog = new ProgressDialog(this);
        if (id == ID_DIALOG_DELETING) {
            mLoadingDialog.setMessage(getResources().getText(
                    mIsPage ? R.string.deleting_page : R.string.deleting_post));
            mLoadingDialog.setCancelable(false);
            return mLoadingDialog;
        } else if (id == ID_DIALOG_SHARE) {
            mLoadingDialog.setMessage(mIsPage ? getString(R.string.share_url_page) : getString(
                    R.string.share_url_post));
            mLoadingDialog.setCancelable(false);
            return mLoadingDialog;
        }
        return super.onCreateDialog(id);
    }



    @Override
    public void onDialogConfirm() {
        mPostList.requestPosts(true);
        mPostList.setRefreshing(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

   /* @Override
    public void onBlogChanged() {
        popPostDetail();
        attemptToSelectPost();
        mPostList.clear();
        mPostList.getPostListAdapter().loadPosts();
        //mPostList.onBlogChanged();
    }*/

    @Override
    public void onSinglePostLoaded() {
        popPostDetail();
    }


    public void setRefreshing(boolean refreshing) {
        mPostList.setRefreshing(refreshing);
    }

    public boolean isDualPane() {
        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.postDetail);
        return fragment != null && fragment.isVisible();
    }




}
