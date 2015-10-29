package com.example.manish.androidcms.ui.comments;

import android.app.Activity;
import android.app.Fragment;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.datasets.CommentTable;
import com.example.manish.androidcms.models.CommentStatus;
import com.example.manish.androidcms.models.Note;
import com.example.manish.androidcms.ui.notifications.NotificationsListFragment;
import com.example.manish.androidcms.ui.reader.actions.ReaderAnim;
import com.example.manish.androidcms.util.AniUtils;
import com.example.manish.androidcms.util.DateTimeUtils;
import com.example.manish.androidcms.util.WPLinkMovementMethod;
import com.example.manish.androidcms.util.widgets.WPNetworkImageView;
import com.example.manish.androidcms.widgets.SuggestionAutoCompleteText;

import org.json.JSONObject;
import org.w3c.dom.Comment;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.EnumSet;

import Rest.RestRequest;

/**
 * Created by Manish on 10/15/2015.
 */
public class CommentDetailFragment extends Fragment  {

    private com.example.manish.androidcms.models.Comment mComment;
    private int mLocalBlogId;

    private CommentActions.OnCommentChangeListener mOnCommentChangeListener;

    private CommentActions.OnCommentActionListener mOnCommentActionListener;

    private static final String KEY_NOTE_ID = "note_id";
    private static final String KEY_LOCAL_BLOG_ID = "local_blog_id";
    private static final String KEY_COMMENT_ID = "comment_id";


    private int mRemoteBlogId;
    private TextView mTxtContent;
    private View mBtnLikeComment;
    private View mBtnModerateComment;
    private ImageView mBtnModerateIcon;
    private TextView mBtnModerateTextView;
    private TextView mBtnSpamComment;
    private TextView mBtnTrashComment;
    private ViewGroup mLayoutReply;
    private SuggestionAutoCompleteText mEditReply;

    private boolean mIsSubmittingReply = false;

    private CommentActions.OnNoteCommentActionListener mOnNoteCommentActionListener;

    private ImageView mBtnLikeIcon;

    private ImageView mImgSubmitReply;

    private TextView mBtnLikeTextView;

    private TextView mTxtStatus;

    private String mRestoredReplyText;

    private Note mNote;
    private ViewGroup mLayoutButtons;

    private EnumSet<Note.EnabledActions> mEnabledActions = EnumSet.allOf(Note.EnabledActions.class);

    /*
         * Used to request a comment from a note using its site and comment ids, rather than build
         * the comment with the content in the note. See showComment()
         */
    private boolean mShouldRequestCommentFromNote = false;


    //At very beginning of the fragment life the method onInflate is called.
    // We have to notice that this method is called only if we define fragment
    // directly in our layout using the tag <fragment>. In this method we can save some
    // configuration parameter and some attributes define in the XML layout file.
    private boolean mIsUsersBlog = false;


    //2.
    //This method is called as soon as the fragment is attached to the father activity
    // and we can this method to store the reference about the activity.
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        if(activity instanceof CommentActions.OnCommentChangeListener)
            mOnCommentChangeListener = (CommentActions.OnCommentChangeListener) activity;

        if(activity instanceof CommentActions.OnCommentActionListener)
            mOnCommentActionListener = (CommentActions.OnCommentActionListener)activity;


    }

    //3.It is one of the most important step, our fragment is in the creation process.
    // This method can be used to start
    //some thread to retrieve data information, maybe from a remote server.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null)
        {
            if(savedInstanceState.getString(KEY_NOTE_ID) != null)
            {

            }
            else
            {
                int localBlogId = savedInstanceState.getInt(KEY_LOCAL_BLOG_ID);
                long commentId = savedInstanceState.getLong(KEY_COMMENT_ID);
                setComment(localBlogId, commentId);

            }
        }
        setHasOptionsMenu(true);

    }

    //4.The onCreateView is the method called when the fragment has to create its view hierarchy.
    // During this method we will inflate our layout inside the fragment as we do for example
    // in the ListView widget. During this phase we cant be
    //sure that our activity is still created so we cant count on it for some operation.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.comment_detail_fragment, container, false);

        mTxtStatus = (TextView) view.findViewById(R.id.text_status);
        mTxtContent = (TextView) view.findViewById(R.id.text_content);

        mLayoutButtons = (ViewGroup) inflater.inflate(R.layout.comment_action_footer, null, false);
        mBtnLikeComment = mLayoutButtons.findViewById(R.id.btn_like);
        mBtnLikeIcon = (ImageView) mLayoutButtons.findViewById(R.id.btn_like_icon);
        mBtnLikeTextView = (TextView)mLayoutButtons.findViewById(R.id.btn_like_text);
        mBtnModerateComment = mLayoutButtons.findViewById(R.id.btn_moderate);
        mBtnModerateIcon = (ImageView)mLayoutButtons.findViewById(R.id.btn_moderate_icon);

        mBtnModerateTextView = (TextView)mLayoutButtons.findViewById(R.id.btn_moderate_text);
        mBtnSpamComment = (TextView) mLayoutButtons.findViewById(R.id.text_btn_spam);
        mBtnTrashComment = (TextView) mLayoutButtons.findViewById(R.id.image_trash_comment);

        setTextDrawable(mBtnSpamComment, R.drawable.ic_action_spam);
        setTextDrawable(mBtnTrashComment, R.drawable.ic_action_trash);

        mLayoutReply = (ViewGroup) view.findViewById(R.id.layout_comment_box);
        mEditReply = (SuggestionAutoCompleteText) mLayoutReply.findViewById(R.id.edit_comment);

        mEditReply.getAutoSaveTextHelper().setUniqueId(String.format("%s%d%d",
                CMS.getLoggedInUsername(getActivity(), CMS.getCurrentBlog()),
                getRemoteBlogId(), getCommentId()));

        mImgSubmitReply = (ImageView) mLayoutReply.findViewById(R.id.image_post_comment);

        // hide comment like button until we know it can be enabled in showCommentForNote()
        mBtnLikeComment.setVisibility(View.GONE);

        // hide moderation buttons until updateModerationButtons() is called
        mLayoutButtons.setVisibility(View.GONE);

        // this is necessary in order for anchor tags in the comment text to be clickable
        mTxtContent.setLinksClickable(true);
        mTxtContent.setMovementMethod(WPLinkMovementMethod.getInstance());

        mEditReply.setHint(R.string.reader_hint_comment_on_comment);
        mEditReply.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
                    submitReply();
                }

                return false;
            }
        });


        if (!TextUtils.isEmpty(mRestoredReplyText)) {
            mEditReply.setText(mRestoredReplyText);
            mRestoredReplyText = null;
        }

        mImgSubmitReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReply();
            }
        });

        mBtnSpamComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moderateComment(CommentStatus.SPAM);
            }
        });

        mBtnTrashComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moderateComment(CommentStatus.TRASH);
            }
        });

        mBtnLikeComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likeComment();
            }
        });

      //  setupSuggestionServiceAndAdapter();

        return view;


    }

    //5.We get notified when the father activity is created and ready in the onActivityCreated.
   // From now on, our activity is active and created and we can use it when we need.

    /*private void setupSuggestionServiceAndAdapter() {
        if (!isAdded()) return;

        mSuggestionServiceConnectionManager = new SuggestionServiceConnectionManager(getActivity(), mRemoteBlogId);
        mSuggestionAdapter = SuggestionUtils.setupSuggestions(mRemoteBlogId, getActivity(), mSuggestionServiceConnectionManager);
        if (mSuggestionAdapter != null) {
            mEditReply.setAdapter(mSuggestionAdapter);
        }
    }*/


    //6.The next step is onStart method. Here we do the common things as in the activity onStart,
    //during this phase our fragment is visible but it isnt still interacting with the user.

    @Override
    public void onStart() {
        super.onStart();
        showComment();
    }

    //.When the fragment is ready to interact
    //with user onResume is called. At the end of this phase our fragment is up and running!!
    @Override
    public void onResume() {
        super.onResume();

        /*LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
                .registerReceiver(mReceiver, new IntentFilter(SuggestionService.ACTION_SUGGESTIONS_LIST_UPDATED));
    */}


    //Then it can happen that the activity is paused and so the activitys onPause is called.
    @Override
    public void onPause() {
        super.onPause();
        // Reset comment if this is from a notification
        if (mNote != null) {
            mComment = null;
        }
        EditTextUtils.hideSoftInput(mEditReply);
       // LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
               // .unregisterReceiver(mReceiver);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.comment_detail, menu);
        if (!canEdit()) {
            menu.removeItem(R.id.menu_edit_comment);
        }
    }

    private boolean canEdit() {
        return (mLocalBlogId > 0 && canModerate());
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.menu_edit_comment) {
            //();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //After it, if the system decides to dismiss our fragment it calls onDestroy method. Here we should release all the connection active and so on because our fragment is close to die.
    // Even if it is during the destroy phase it is still attached to the father activity.
    @Override
    public void onDestroy() {
        /*if (mSuggestionServiceConnectionManager != null) {
            mSuggestionServiceConnectionManager.unbindFromService();
        }*/
        super.onDestroy();
    }

    // Like or unlike a comment via the REST API
    private void likeComment()
    {
        if (mNote == null) return;

        toggleLikeButton(!mBtnLikeComment.isActivated());

        ReaderAnim.animateLikeButton(mBtnLikeIcon, mBtnLikeComment.isActivated());

        boolean commentWasUnapproved = false;


        /*if (mNotificationsDetailListFragment != null && mComment != null) {
            // Optimistically set comment to approved when liking an unapproved comment
            // WP.com will set a comment to approved if it is liked while unapproved
            if (mBtnLikeComment.isActivated() && mComment.getStatusEnum() == CommentStatus.
            UNAPPROVED) {
                mComment.setStatus(CommentStatus.toString(CommentStatus.APPROVED));
                mNotificationsDetailListFragment.refreshBlocksForCommentStatus(CommentStatus.APPROVED);
                setModerateButtonForStatus(CommentStatus.APPROVED);
                commentWasUnapproved = true;
            }
        }*/

        final boolean commentStatusShouldRevert = commentWasUnapproved;

        CMS.getRestClientUtils().likeComment(String.valueOf(mNote.getSiteId()),
                String.valueOf(mNote.getCommentId()),
                mBtnLikeComment.isActivated(),
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response != null && !response.optBoolean("success")) {
                            if (!isAdded()) return;

                            // Failed, so switch the button state back
                            toggleLikeButton(!mBtnLikeComment.isActivated());

                            if (commentStatusShouldRevert) {
                                setCommentStatusUnapproved();
                            }
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (!isAdded()) return;

                        toggleLikeButton(!mBtnLikeComment.isActivated());

                        if (commentStatusShouldRevert) {
                            setCommentStatusUnapproved();
                        }
                    }
                });

    }

    private void setCommentStatusUnapproved() {
        mComment.setStatus(CommentStatus.toString(CommentStatus.UNAPPROVED));
       // mNotificationsDetailListFragment.refreshBlocksForCommentStatus(CommentStatus.UNAPPROVED);
        setModerateButtonForStatus(CommentStatus.UNAPPROVED);
    }

    private void toggleLikeButton(boolean isLiked) {
        if (isLiked) {
            mBtnLikeTextView.setText(getResources().getString(R.string.mnu_comment_liked));
            mBtnLikeTextView.setTextColor(getResources().getColor(R.color.orange_medium));
            mBtnLikeIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_like_active));
            mBtnLikeComment.setActivated(true);
        } else {
            mBtnLikeTextView.setText(getResources().getString(R.string.reader_label_like));
            mBtnLikeTextView.setTextColor(getResources().getColor(R.color.calypso_blue));
            mBtnLikeIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_like));
            mBtnLikeComment.setActivated(false);
        }
    }

    private void submitReply()
    {
        if(!isAdded() || mIsSubmittingReply )
            return;

        if(!NetworkUtils.checkConnection(getActivity()))
            return;

        final String replyText = EditTextUtils.getText(mEditReply);
        if (TextUtils.isEmpty(replyText))
            return;

        // disable editor, hide soft keyboard, hide submit icon,
        // and show progress spinner while submitting
        mEditReply.setEnabled(false);
        EditTextUtils.hideSoftInput(mEditReply);
        mImgSubmitReply.setVisibility(View.GONE);

        final ProgressBar progress = (ProgressBar) getView().findViewById(R.id.progress_submit_comment);
        progress.setVisibility(View.VISIBLE);


        CommentActions.CommentActionListener actionListener = new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeded) {
                mIsSubmittingReply = false;

                if(succeded && mOnCommentChangeListener != null)
                    mOnCommentChangeListener.onCommentChanged(CommentActions.ChangedFrom.COMMENT_DETAIL,
                            CommentActions.ChangeType.REPLIED);

                if(isAdded())
                {
                    mEditReply.setEnabled(true);
                    mImgSubmitReply.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.GONE);
                    updateStatusViews();
                    if (succeded) {
                        ToastUtils.showToast(getActivity(), getString(R.string.note_reply_successful));
                        mEditReply.setText(null);
                        mEditReply.getAutoSaveTextHelper().clearSavedText(mEditReply);
                    } else {
                        ToastUtils.showToast(getActivity(), R.string.reply_failed, ToastUtils.Duration.LONG);
                        // refocus editor on failure and show soft keyboard
                        EditTextUtils.showSoftInput(mEditReply);
                    }
                }
            }
        };

        mIsSubmittingReply = true;

        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_REPLIED_TO);

        if (mNote != null) {
            if (mShouldRequestCommentFromNote) {
                //CommentActions.submitReplyToCommentRestApi(mNote.getSiteId(), mComment.commentID, replyText, actionListener);
            } else {
                //CommentActions.submitReplyToCommentNote(mNote, replyText, actionListener);
            }
        } else {
            CommentActions.submitReplyToComment(mLocalBlogId, mComment, replyText, actionListener);
        }

    }

    /*
     * approve, unapprove, spam, or trash the current comment
     */
    private void moderateComment(final CommentStatus newStatus)
    {
        if(!isAdded() || !hasComment())
        {
            return;
        }

        if (!NetworkUtils.checkConnection(getActivity()))
            return;

        // Fire the appropriate listener if we have one
        if (mNote != null && mOnNoteCommentActionListener != null) {
           // mOnNoteCommentActionListener.onModerateCommentForNote(mNote, newStatus);
            //trackModerationFromNotification(newStatus);
            return;
        } else if (mOnCommentActionListener != null) {
            mOnCommentActionListener.onModerateComment(mLocalBlogId, mComment, newStatus);
            return;
        }

        if (mNote == null) return;

        // Basic moderation support, currently only used when this Fragment is in a CommentDetailActivity
        // Uses WP.com REST API and requires a note object
        final CommentStatus oldStatus = mComment.getStatusEnum();
        mComment.setStatus(CommentStatus.toString(newStatus));
        updateStatusViews();
        CommentActions.moderateCommentRestApi(mNote.getSiteId(), mComment.commentID, newStatus, new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!isAdded()) return;

                if (!succeeded) {
                    mComment.setStatus(CommentStatus.toString(oldStatus));
                    updateStatusViews();
                    ToastUtils.showToast(getActivity(), R.string.error_moderate_comment);
                }
            }
        });

    }

    private int getRemoteBlogId() {
        return mRemoteBlogId;
    }

    /*
     * sets the drawable for moderation buttons
     */
    private void setTextDrawable(final TextView view, int resId) {
        view.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(resId),
                null, null);
    }

    /*
     * used when called from comment list
     */
    static CommentDetailFragment newInstance(int localBlogId,
                                             long commentId)
    {
        CommentDetailFragment fragment = new CommentDetailFragment();
        fragment.setComment(localBlogId, commentId);
        return fragment;

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (hasComment()) {
            outState.putInt(KEY_LOCAL_BLOG_ID, getLocalBlogId());
            outState.putLong(KEY_COMMENT_ID, getCommentId());
        }

        if (mNote != null) {
            outState.putString(KEY_NOTE_ID, mNote.getId());
        }
    }

    private int getLocalBlogId() {
        return mLocalBlogId;
    }

    long getCommentId() {
        return (mComment != null ? mComment.commentID : 0);
    }


    void setComment(int localBlogId, long commentId) {
        setComment(localBlogId, CommentTable.getComment(localBlogId, commentId));
    }

    private void setComment(int localBlogId, final com.example.manish.androidcms.models.Comment comment)
    {
        mComment = comment;
        mLocalBlogId = localBlogId;

        // is this comment on one of the user's blogs? it won't be if this was displayed from a
        // notification about a reply to a comment this user posted on someone else's blog
        mIsUsersBlog = (comment != null && CMS.cmsDB.isLocalBlogIdInDatabase(mLocalBlogId));

        if (mIsUsersBlog)
            mRemoteBlogId = CMS.cmsDB.getRemoteBlogIdForLocalTableBlogId(mLocalBlogId);

        if (isAdded())
            showComment();
    }


    /* display the current comment
    */
    private void showComment()
    {
        if(!isAdded() || getView() == null)
            return;

        // these two views contain all the other views except the progress bar
        final ScrollView scrollView = (ScrollView)getView().findViewById(R.id.scroll_view);
        final View layoutBottom = getView().findViewById(R.id.layout_bottom);

        // hide container views when comment is null
        // (will happen when opened from a notification)
        if (mComment == null) {

            return;
        }

        scrollView.setVisibility(View.VISIBLE);
        layoutBottom.setVisibility(View.VISIBLE);

        // Add action buttons footer
        if((mNote == null || mShouldRequestCommentFromNote)

            && mLayoutButtons.getParent() == null)
        {
            ViewGroup commentContentLayout = (ViewGroup) getView().findViewById(
                    R.id.comment_content_container
            );

            commentContentLayout.addView(mLayoutButtons);

        }

        final WPNetworkImageView imgAvatar = (WPNetworkImageView) getView().
                findViewById(R.id.image_avatar);
        final TextView txtName = (TextView) getView().findViewById(R.id.text_name);
        final TextView txtDate = (TextView) getView().findViewById(R.id.text_date);

        txtName.setText(mComment.hasAuthorName() ? mComment.getAuthorName() : getString(R.string.anonymous));
        txtDate.setText(DateTimeUtils.javaDateToTimeSpan(mComment.getDatePublished()));


        int maxImageSz = getResources().getDimensionPixelSize(R.dimen.reader_comment_max_image_size);
        CommentUtils.displayHtmlComment(mTxtContent, mComment.getCommentText(), maxImageSz);

        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
        if (mComment.hasProfileImageUrl()) {
            imgAvatar.setImageUrl(GravatarUtils.fixGravatarUrl(mComment.getProfileImageUrl(), avatarSz), WPNetworkImageView.ImageType.AVATAR);
        } else if (mComment.hasAuthorEmail()) {
            String avatarUrl = GravatarUtils.gravatarFromEmail(mComment.getAuthorEmail(), avatarSz);
            imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
        } else {
            imgAvatar.setImageUrl(null, WPNetworkImageView.ImageType.AVATAR);
        }

        updateStatusViews();

    }


    private boolean hasComment() {
        return (mComment != null);
    }

    /*
     * update the text, drawable & click listener for mBtnModerate based on
     * the current status of the comment, show mBtnSpam if the comment isn't
     * already marked as spam, and show the current status of the comment
     */
    private void updateStatusViews()
    {
        if(!isAdded() || !hasComment())
        {
            return;
        }

        final int statusTextResId;      // string resource id for status text
        final int statusColor;          // color for status text

        switch (mComment.getStatusEnum()) {
            case APPROVED:
                statusTextResId = R.string.comment_status_approved;
                statusColor = getActivity().getResources().getColor(R.color.calypso_orange_dark);
                break;
            case UNAPPROVED:
                statusTextResId = R.string.comment_status_unapproved;
                statusColor = getActivity().getResources().getColor(R.color.calypso_orange_dark);
                break;
            case SPAM:
                statusTextResId = R.string.comment_status_spam;
                statusColor = getActivity().getResources().getColor(R.color.comment_status_spam);
                break;
            case TRASH:
            default:
                statusTextResId = R.string.comment_status_trash;
                statusColor = getActivity().getResources().getColor(R.color.comment_status_spam);
                break;
        }

        if (mNote != null && canLike()) {
            mBtnLikeComment.setVisibility(View.VISIBLE);

           // toggleLikeButton(mNote.hasLikedComment());
        } else {
            mBtnLikeComment.setVisibility(View.GONE);
        }

        // comment status is only shown if this comment is from one of this user's blogs and the
        // comment hasn't been approved
        if (mIsUsersBlog && mComment.getStatusEnum() != CommentStatus.APPROVED) {
            mTxtStatus.setText(getString(statusTextResId).toUpperCase());
            mTxtStatus.setTextColor(statusColor);
            if (mTxtStatus.getVisibility() != View.VISIBLE) {
                mTxtStatus.clearAnimation();
                AniUtils.fadeIn(mTxtStatus);
            }
        } else {
            mTxtStatus.setVisibility(View.GONE);
        }

        if(canModerate())
        {
            setModerateButtonForStatus(mComment.getStatusEnum());
            mBtnModerateComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!hasComment() || !isAdded() || !NetworkUtils.checkConnection(getActivity())) {
                        return;
                    }

                    CommentStatus newStatus = CommentStatus.APPROVED;
                    if (mComment.getStatusEnum() == CommentStatus.APPROVED) {
                        newStatus = CommentStatus.UNAPPROVED;
                    }

                    mComment.setStatus(newStatus.toString());
                    setModerateButtonForStatus(newStatus);
                    AniUtils.startAnimation(mBtnModerateIcon,
                            R.anim.notifications_button_scale);

                    moderateComment(newStatus);

                }
            });
            mBtnModerateComment.setVisibility(View.VISIBLE);


        }
        else {
            mBtnModerateComment.setVisibility(View.GONE);
        }

        if (canMarkAsSpam()) {
            mBtnSpamComment.setVisibility(View.VISIBLE);
            if (mComment.getStatusEnum() == CommentStatus.SPAM) {
                mBtnSpamComment.setText(R.string.mnu_comment_unspam);
            } else {
                mBtnSpamComment.setText(R.string.mnu_comment_spam);
            }
        } else {
            mBtnSpamComment.setVisibility(View.GONE);
        }

        if (canTrash()) {
            mBtnTrashComment.setVisibility(View.VISIBLE);
        } else {
            mBtnTrashComment.setVisibility(View.GONE);
        }

        mLayoutButtons.setVisibility(View.VISIBLE);
    }

    private boolean canTrash() {
        return canModerate();
    }

    private boolean canMarkAsSpam() {
        return (mEnabledActions != null && mEnabledActions.contains(Note.EnabledActions.ACTION_SPAM));
    }

    private void setModerateButtonForStatus(CommentStatus status) {
        if (status == CommentStatus.APPROVED) {
            mBtnModerateIcon.setImageResource(R.drawable.ic_action_approve_active);
            mBtnModerateTextView.setText(R.string.comment_status_approved);
            mBtnModerateTextView.setTextColor(getActivity().getResources().getColor(R.color.calypso_orange_dark));
        } else {
            mBtnModerateIcon.setImageResource(R.drawable.ic_action_approve);
            mBtnModerateTextView.setText(R.string.mnu_comment_approve);
            mBtnModerateTextView.setTextColor(getActivity().getResources().getColor(R.color.calypso_blue));
        }
    }

    /*
     * does user have permission to moderate/reply/spam this comment?
     */
    private boolean canModerate() {
        if (mEnabledActions == null)
            return false;
        return (mEnabledActions.contains(Note.EnabledActions.ACTION_APPROVE)
                || mEnabledActions.contains(Note.EnabledActions.ACTION_UNAPPROVE));
    }

    /*private void toggleLikeButton(boolean isLiked) {
        if (isLiked) {
            mBtnLikeTextView.setText(getResources().getString(R.string.mnu_comment_liked));
            mBtnLikeTextView.setTextColor(getResources().getColor(R.color.orange_medium));
            mBtnLikeIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_like_active));
            mBtnLikeComment.setActivated(true);
        } else {
            mBtnLikeTextView.setText(getResources().getString(R.string.reader_label_like));
            mBtnLikeTextView.setTextColor(getResources().getColor(R.color.calypso_blue));
            mBtnLikeIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_like));
            mBtnLikeComment.setActivated(false);
        }
    }*/

    private boolean canLike() {
        return (!mShouldRequestCommentFromNote && mEnabledActions != null && mEnabledActions.contains(Note.EnabledActions.ACTION_LIKE));
    }
}
