package com.example.manish.androidcms.ui.comments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cocosw.undobar.UndoBarController;
import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.Comment;
import com.example.manish.androidcms.models.CommentStatus;
import com.example.manish.androidcms.models.Note;
import com.example.manish.androidcms.ui.CMSDrawerActivity;
import com.example.manish.androidcms.ui.notifications.NotificationFragment;

import org.wordpress.android.util.ToastUtils;

/**
 * Created by Manish on 10/12/2015.
 */
public class CommentsActivity extends CMSDrawerActivity
    implements CommentsListFragment.OnCommentSelectedListener,
        CommentActions.OnCommentChangeListener,
        CommentActions.OnCommentActionListener,
        NotificationFragment.OnPostClickListener

{

    static final String KEY_AUTO_REFRESHED = "has_auto_refreshed";
    static final String KEY_EMPTY_VIEW_MESSAGE = "empty_view_message";

    private static final String KEY_SELECTED_COMMENT_ID = "selected_comment_id";

    private long mSelectedCommentId;



    /*
     * called from comment detail when user taps a link to a post - show the post in a
     * reader detail fragment
     */
    @Override
    public void onPostClicked(Note note, int remoteBlogId, int postId) {
        showReaderFragment(remoteBlogId, postId);
    }

    void showReaderFragment(long remoteBlogId, long postId)
    {
        FragmentManager fm = getFragmentManager();
        fm.executePendingTransactions();

        //Fragment fragment =
    }
    // It is one of the most important step, our fragment is in the creation process.
    // This method can be
    // used to start some thread to retrieve data information, maybe from a remote server.

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(null);

        createMenuDrawer(R.layout.comment_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(getString(R.string.tab_comments));
        }

        getFragmentManager().addOnBackStackChangedListener(mOnBackStackChangedListener);

        restoreSavedInstance(savedInstanceState);

    }

    private void restoreSavedInstance(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            getIntent().putExtra(KEY_AUTO_REFRESHED, savedInstanceState.getBoolean(KEY_AUTO_REFRESHED));
            getIntent().putExtra(KEY_EMPTY_VIEW_MESSAGE, savedInstanceState.getString(KEY_EMPTY_VIEW_MESSAGE));

            // restore the selected comment
            long commentId = savedInstanceState.getLong(KEY_SELECTED_COMMENT_ID);
            if (commentId != 0) {
                onCommentSelected(commentId);
            }
            // restore the post detail fragment if one was selected
            /*BlogPairId selectedPostId = (BlogPairId) savedInstanceState.get(KEY_SELECTED_POST_ID);
            if (selectedPostId != null) {
                showReaderFragment(selectedPostId.getRemoteBlogId(), selectedPostId.getId());
            }*/
        }
    }

    /*
 * called from comment list when user taps a comment
 */
    @Override
    public void onCommentSelected(long commentId)
    {
        mSelectedCommentId = commentId;
        FragmentManager fm = getFragmentManager();
        if(fm == null)
        {
            return;
        }

        fm.executePendingTransactions();
        CommentsListFragment listFragment = getListFragment();

        FragmentTransaction ft = fm.beginTransaction();
        String tagForFragment = getString(R.string.fragment_tag_comment_detail);
        CommentDetailFragment detailFragment = CommentDetailFragment.newInstance(
                CMS.getCurrentLocalTableBlogId(), commentId);

        ft.add(R.id.layout_fragment_container, detailFragment, tagForFragment)
                .addToBackStack(tagForFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

        if(listFragment != null)
        {
            ft.hide(listFragment);
        }

        ft.commitAllowingStateLoss();

        if(getDrawerToggle() != null)
        {
            getDrawerToggle().setDrawerIndicatorEnabled(false);
        }


    }


    private CommentsListFragment getListFragment()
    {
        Fragment fragment = getFragmentManager().findFragmentByTag(getString(R.string.fragment_tag_comment_list));

        if(fragment == null)
        {
            return null;
        }
        return  (CommentsListFragment) fragment;
    }


    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener =
            new FragmentManager.OnBackStackChangedListener() {
                public void onBackStackChanged() {
                    if (getDrawerToggle() != null) {
                        int backStackEntryCount = getFragmentManager().getBackStackEntryCount();
                        if (backStackEntryCount == 0) {
                            getDrawerToggle().setDrawerIndicatorEnabled(true);
                        } else {
                            getDrawerToggle().setDrawerIndicatorEnabled(false);
                        }
                    }
                }
            };

    /*
     * reload the comment list from existing data
     */
     private void reloadCommentList()
     {
         CommentsListFragment listFragment = getListFragment();
         if(listFragment != null)
         {
             listFragment.setRefreshing(true);
             listFragment.updateComments(false);
         }
     }

    @Override
    public void onModerateComment(final int accountId, final Comment comment,
                                  final CommentStatus newStatus)
    {
        FragmentManager fm = getFragmentManager();
        if(fm.getBackStackEntryCount() > 0)
        {
            fm.popBackStack();
        }

        if(newStatus == CommentStatus.APPROVED || newStatus == CommentStatus.UNAPPROVED)
        {
            getListFragment().setCommentIsModerating(comment.commentID, true);

            CommentActions.moderateComment(accountId, comment, newStatus,
                    new CommentActions.CommentActionListener() {
                        @Override
                        public void onActionResult(boolean succeded) {
                            if (isFinishing() || !hasListFragment())
                            {
                                return;
                            }

                            getListFragment().setCommentIsModerating(comment.commentID,
                                    false);

                            if(succeded)
                            {
                                updateMenuDrawer();
                                getListFragment().updateComments(false);
                            }else {
                                ToastUtils.showToast(CommentsActivity.this,
                                        R.string.error_moderate_comment,
                                        ToastUtils.Duration.LONG
                                );
                            }
                        }
                    });
        }

        else if (newStatus == CommentStatus.SPAM || newStatus == CommentStatus.TRASH)
        {
            // Remove comment from comments list
            getListFragment().removeComment(comment);
            getListFragment().setCommentIsModerating(comment.commentID, true);

            new UndoBarController.UndoBar(this).message(newStatus == CommentStatus.TRASH ?
                    R.string.comment_trashed :

                    R.string.comment_spammed)
                    .listener(new UndoBarController.AdvancedUndoListener()
                    {
                        @Override
                        public void onHide(Parcelable parcelable) {

                            CommentActions.moderateComment(accountId, comment, newStatus,
                                    new CommentActions.CommentActionListener()
                                    {
                                        @Override
                                        public void onActionResult(boolean succeeded)
                                        {
                                            if(isFinishing() || !hasListFragment())
                                            {
                                                return;
                                            }

                                            getListFragment().setCommentIsModerating(comment.commentID,
                                                    false);

                                            if(!succeeded)
                                            {
                                                getListFragment().loadComments();
                                                ToastUtils.showToast(CommentsActivity.this,
                                                        R.string.error_moderate_comment,
                                                        ToastUtils.Duration.LONG
                                                );
                                            }
                                            else {
                                                updateMenuDrawer();
                                            }

                                        }
                                    });
                        }

                        @Override
                        public void onClear(Parcelable[] token) {
                            //noop
                        }

                        @Override
                        public void onUndo(Parcelable parcelable) {
                            getListFragment().setCommentIsModerating(comment.commentID, false);
                            // On undo load from the db to show the comment again
                            getListFragment().loadComments();
                        }
                    }).show();

        }
    }

    private boolean hasListFragment() {
        return (getListFragment() != null);
    }



    @Override
    public void onCommentChanged(CommentActions.ChangedFrom changedFrom,
                                 CommentActions.ChangeType changeType)
    {
        if (changedFrom == CommentActions.ChangedFrom.COMMENT_DETAIL
                && changeType == CommentActions.ChangeType.EDITED) {
            reloadCommentList();
        }
    }
}
