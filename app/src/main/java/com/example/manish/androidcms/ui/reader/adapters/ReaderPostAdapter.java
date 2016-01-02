package com.example.manish.androidcms.ui.reader.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.manish.androidcms.R;
import com.example.manish.androidcms.datasets.ReaderPostTable;
import com.example.manish.androidcms.models.ReaderPost;
import com.example.manish.androidcms.models.ReaderPostList;
import com.example.manish.androidcms.models.ReaderTag;
import com.example.manish.androidcms.ui.reader.ReaderActivityLauncher;
import com.example.manish.androidcms.ui.reader.ReaderConstants;
import com.example.manish.androidcms.ui.reader.ReaderInterfaces;
import com.example.manish.androidcms.ui.reader.ReaderTypes;
import com.example.manish.androidcms.ui.reader.actions.ReaderActions;
import com.example.manish.androidcms.ui.reader.actions.ReaderAnim;
import com.example.manish.androidcms.ui.reader.actions.ReaderBlogActions;
import com.example.manish.androidcms.ui.reader.actions.ReaderPostActions;
import com.example.manish.androidcms.ui.reader.views.ReaderFollowButton;
import com.example.manish.androidcms.ui.reader.views.ReaderIconCountView;
import com.example.manish.androidcms.util.DateTimeUtils;
import com.example.manish.androidcms.util.widgets.WPNetworkImageView;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ToastUtils;

/**
 * Created by Manish on 12/30/2015.
 */
public class ReaderPostAdapter extends RecyclerView.Adapter<ReaderPostAdapter.ReaderPostViewHolder> {


    private ReaderTag mCurrentTag;
    private long mCurrentBlogId;

    private final int mPhotonWidth;
    private final int mPhotonHeight;
    private final int mAvatarSz;
    private final int mMarginLarge;

    private boolean mCanRequestMorePosts = false;

    private final ReaderTypes.ReaderPostListType mPostListType;

    // the large "tbl_posts.text" column is unused here, so skip it when querying
    private static final boolean EXCLUDE_TEXT_COLUMN = true;
    private static final int MAX_ROWS = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY;

    private ReaderInterfaces.OnPostSelectedListener mPostSelectedListener;
    private ReaderInterfaces.OnTagSelectedListener mOnTagSelectedListener;
    private ReaderInterfaces.OnPostPopupListener mOnPostPopupListener;
    private ReaderInterfaces.RequestReblogListener mReblogListener;
    private ReaderInterfaces.DataLoadedListener mDataLoadedListener;
    private ReaderActions.DataRequestedListener mDataRequestedListener;

    private final ReaderPostList mPosts = new ReaderPostList();


    public void setOnPostSelectedListener(ReaderInterfaces.OnPostSelectedListener listener) {
        mPostSelectedListener = listener;
    }

    public void setOnTagSelectedListener(ReaderInterfaces.OnTagSelectedListener listener) {
        mOnTagSelectedListener = listener;
    }

    public void setOnDataLoadedListener(ReaderInterfaces.DataLoadedListener listener) {
        mDataLoadedListener = listener;
    }

    public void setOnDataRequestedListener(ReaderActions.DataRequestedListener listener) {
        mDataRequestedListener = listener;
    }

    public void refresh() {
        loadPosts();
    }

    public void setOnReblogRequestedListener(ReaderInterfaces.RequestReblogListener listener) {
        mReblogListener = listener;
    }

    public void setOnPostPopupListener(ReaderInterfaces.OnPostPopupListener onPostPopupListener) {
        mOnPostPopupListener = onPostPopupListener;
    }
    class ReaderPostViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final TextView txtText;
        private final TextView txtBlogName;
        private final TextView txtDate;
        private final TextView txtTag;

        private final ReaderIconCountView commentCount;
        private final ReaderIconCountView likeCount;
        private final ReaderFollowButton followButton;

        private final ImageView imgBtnReblog;
        private final ImageView imgMore;

        private final WPNetworkImageView imgFeatured;
        private final WPNetworkImageView imgAvatar;

        private final ViewGroup layoutPostHeader;

        public ReaderPostViewHolder(View itemView) {
            super(itemView);

            txtTitle = (TextView) itemView.findViewById(R.id.text_title);
            txtText = (TextView) itemView.findViewById(R.id.text_excerpt);
            txtBlogName = (TextView) itemView.findViewById(R.id.text_blog_name);
            txtDate = (TextView) itemView.findViewById(R.id.text_date);
            txtTag = (TextView) itemView.findViewById(R.id.text_tag);

            commentCount = (ReaderIconCountView) itemView.findViewById(R.id.count_comments);
            likeCount = (ReaderIconCountView) itemView.findViewById(R.id.count_likes);
            followButton = (ReaderFollowButton) itemView.findViewById(R.id.follow_button);

            imgFeatured = (WPNetworkImageView) itemView.findViewById(R.id.image_featured);
            imgAvatar = (WPNetworkImageView) itemView.findViewById(R.id.image_avatar);
            imgMore = (ImageView) itemView.findViewById(R.id.image_more);

            layoutPostHeader = (ViewGroup) itemView.findViewById(R.id.layout_post_header);

            imgBtnReblog = (ImageView) itemView.findViewById(R.id.image_reblog_btn);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                imgBtnReblog.setBackgroundResource(R.drawable.ripple_oval);
            }
        }
    }

    // used when the viewing tagged posts
    public void setCurrentTag(ReaderTag tag) {
        if (!ReaderTag.isSameTag(tag, mCurrentTag)) {
            mCurrentTag = tag;
            reload();
        }
    }

    /*
     * AsyncTask to load posts in the current tag
     */
    private boolean mIsTaskRunning = false;
    /*
     * same as refresh() above but first clears the existing posts
     */
    void reload() {
        clear();
        loadPosts();
    }

    private void loadPosts()
    {
        if (mIsTaskRunning) {
            AppLog.w(AppLog.T.READER, "reader posts task already running");
            return;
        }

        new LoadPostsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    private class LoadPostsTask extends AsyncTask<Void, Void, Boolean>
    {
        ReaderPostList allPosts;

        @Override
        protected void onPreExecute()
        {
            mIsTaskRunning = true;
        }

        @Override
        protected void onCancelled() {
            mIsTaskRunning = false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            final int numExisting;
            switch (getPostListType()) {
                case TAG_PREVIEW:
                case TAG_FOLLOWED:
                    allPosts = ReaderPostTable.getPostsWithTag(mCurrentTag, MAX_ROWS,
                            EXCLUDE_TEXT_COLUMN);
                    numExisting = ReaderPostTable.getNumPostsWithTag(mCurrentTag);
                    break;
                case BLOG_PREVIEW:
                   // allPosts = ReaderPostTable.getPostsInBlog(mCurrentBlogId, MAX_ROWS, EXCLUDE_TEXT_COLUMN);
                    numExisting = ReaderPostTable.getNumPostsInBlog(mCurrentBlogId);
                    break;
                default:
                    return false;
            }

            if (mPosts.isSameList(allPosts)) {
                return false;
            }

            // if we're not already displaying the max # posts, enable requesting more when
            // the user scrolls to the end of the list
            mCanRequestMorePosts = (numExisting < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY);

            return true;
        }

    }

    private void clear() {
        if (!mPosts.isEmpty()) {
            mPosts.clear();
            notifyDataSetChanged();
        }
    }

    ReaderTypes.ReaderPostListType getPostListType() {
        return (mPostListType != null ? mPostListType : ReaderTypes.DEFAULT_POST_LIST_TYPE);
    }

    public boolean isCurrentTag(ReaderTag tag) {
        return ReaderTag.isSameTag(tag, mCurrentTag);
    }

    @Override
    public ReaderPostViewHolder onCreateViewHolder(ViewGroup parent, int position) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reader_cardview_post, parent, false);
        return new ReaderPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ReaderPostViewHolder holder, final int position) {
        final ReaderPost post = mPosts.get(position);
        ReaderTypes.ReaderPostListType postListType = getPostListType();

        holder.txtTitle.setText(post.getTitle());
        holder.txtDate.setText(DateTimeUtils.javaDateToTimeSpan(post.getDatePublished()));

        // hide the post header (avatar, blog name & follow button)
        // if we're showing posts
        // in a specific blog
        if (postListType == ReaderTypes.ReaderPostListType.BLOG_PREVIEW) {
            holder.layoutPostHeader.setVisibility(View.GONE);
        }
        else
        {
            holder.layoutPostHeader.setVisibility(View.VISIBLE);

            holder.imgAvatar.setImageUrl(post.getPostAvatarForDisplay(mAvatarSz),
                    WPNetworkImageView.ImageType.AVATAR);
            if (post.hasBlogName()) {
                holder.txtBlogName.setText(post.getBlogName());
            } else if (post.hasAuthorName()) {
                holder.txtBlogName.setText(post.getAuthorName());
            } else {
                holder.txtBlogName.setText(null);
            }

            // follow/following
            holder.followButton.setIsFollowed(post.isFollowedByCurrentUser);
            holder.followButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollow((ReaderFollowButton) v, position);
                }
            });

            // show blog/feed preview when avatar is tapped
            holder.imgAvatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ReaderActivityLauncher.showReaderBlogPreview(view.getContext(), post);
                }
            });

        }
        if (post.hasExcerpt()) {
            holder.txtText.setVisibility(View.VISIBLE);
            holder.txtText.setText(post.getExcerpt());
        } else {
            holder.txtText.setVisibility(View.GONE);
        }

        final int titleMargin;
        if (post.hasFeaturedImage()) {
            final String imageUrl = post.getFeaturedImageForDisplay(mPhotonWidth, mPhotonHeight);
            holder.imgFeatured.setImageUrl(imageUrl, WPNetworkImageView.ImageType.PHOTO);
            holder.imgFeatured.setVisibility(View.VISIBLE);
            titleMargin = mMarginLarge;
        } else if (post.hasFeaturedVideo()) {
            holder.imgFeatured.setVideoUrl(post.postId, post.getFeaturedVideo());
            holder.imgFeatured.setVisibility(View.VISIBLE);
            titleMargin = mMarginLarge;
        } else {
            holder.imgFeatured.setVisibility(View.GONE);
            titleMargin = (holder.layoutPostHeader.getVisibility() == View.VISIBLE ? 0 : mMarginLarge);
        }

        // set the top margin of the title based on whether there's a featured image and post header
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.txtTitle.getLayoutParams();
        params.topMargin = titleMargin;

        // show the best tag for this post
        final String tagToDisplay = (mCurrentTag != null ? post.getTagForDisplay(mCurrentTag.getTagName()) : null);
        if (!TextUtils.isEmpty(tagToDisplay)) {
            holder.txtTag.setText(tagToDisplay);
            holder.txtTag.setVisibility(View.VISIBLE);
            holder.txtTag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnTagSelectedListener != null) {
                        mOnTagSelectedListener.onTagSelected(tagToDisplay);
                    }
                }
            });
        } else {
            holder.txtTag.setVisibility(View.GONE);
            holder.txtTag.setOnClickListener(null);
        }

        // likes, comments & reblogging - supported by wp posts only
        boolean showLikes = post.isWP() && post.isLikesEnabled;
        boolean showComments = post.isWP() && (post.isCommentsOpen || post.numReplies > 0);

        if (showLikes || showComments) {
            showCounts(holder, post, false);
        }

        if (showLikes) {
            holder.likeCount.setSelected(post.isLikedByCurrentUser);
            holder.likeCount.setVisibility(View.VISIBLE);
            holder.likeCount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleLike(v.getContext(), holder, position);
                }
            });
        } else {
            holder.likeCount.setVisibility(View.GONE);
            holder.likeCount.setOnClickListener(null);
        }

        if (showComments) {
            holder.commentCount.setVisibility(View.VISIBLE);
            holder.commentCount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                  //  ReaderActivityLauncher.showReaderComments(v.getContext(), post);
                }
            });
        } else {
            holder.commentCount.setVisibility(View.GONE);
            holder.commentCount.setOnClickListener(null);
        }

        /*if (post.canReblog()) {
            showReblogStatus(holder.imgBtnReblog, post.isRebloggedByCurrentUser);
            holder.imgBtnReblog.setVisibility(View.VISIBLE);
            if (!post.isRebloggedByCurrentUser) {
                holder.imgBtnReblog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ReaderAnim.animateReblogButton(holder.imgBtnReblog);
                        if (mReblogListener != null) {
                            mReblogListener.onRequestReblog(post, v);
                        }
                    }
                });
            } else {
                holder.imgBtnReblog.setOnClickListener(null);
            }
        } else {
            // use INVISIBLE rather than GONE to ensure container maintains the same height
            holder.imgBtnReblog.setVisibility(View.INVISIBLE);
            holder.imgBtnReblog.setOnClickListener(null);
        }*/

        /*// more menu with "block this blog" only shows for public wp posts in followed tags
        if (post.isWP() && !post.isPrivate && postListType == ReaderTypes.ReaderPostListType.TAG_FOLLOWED) {
            holder.imgMore.setVisibility(View.VISIBLE);
            holder.imgMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOnPostPopupListener != null) {
                        mOnPostPopupListener.onShowPostPopup(view, post);
                    }
                }
            });
        } else {
            holder.imgMore.setVisibility(View.GONE);
            holder.imgMore.setOnClickListener(null);
        }*/

        /*// if we're nearing the end of the posts, fire request to load more
        if (mCanRequestMorePosts && mDataRequestedListener != null && (position >= getItemCount() - 1)) {
            mDataRequestedListener.onRequestData();
        }

        if (mPostSelectedListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPostSelectedListener.onPostSelected(post.blogId, post.postId);
                }
            });
        }*/
    }

    public ReaderPostAdapter(Context context, ReaderTypes.ReaderPostListType postListType) {
        super();

        mPostListType = postListType;
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
        mMarginLarge = context.getResources().getDimensionPixelSize(R.dimen.margin_large);

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int cardSpacing = context.getResources().getDimensionPixelSize(R.dimen.reader_card_spacing);
        mPhotonWidth = displayWidth - (cardSpacing * 2);
        mPhotonHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_featured_image_height);

        setHasStableIds(true);
    }

    /*
     * triggered when user taps the like button (textView)
     */
    private void toggleLike(Context context, ReaderPostViewHolder holder, int position) {
        ReaderPost post = getItem(position);
        if (post == null) {
            return;
        }

        boolean isCurrentlyLiked = ReaderPostTable.isPostLikedByCurrentUser(post);
        boolean isAskingToLike = !isCurrentlyLiked;
        ReaderAnim.animateLikeButton(holder.likeCount.getImageView(), isAskingToLike);

        if (!ReaderPostActions.performLikeAction(post, isAskingToLike)) {
            ToastUtils.showToast(context, R.string.reader_toast_err_generic);
            return;
        }

        if (isAskingToLike) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LIKED_ARTICLE);
        }

        // update post in array and on screen
        ReaderPost updatedPost = ReaderPostTable.getPost(post.blogId, post.postId, true);
        if (updatedPost != null) {
            mPosts.set(position, updatedPost);
            holder.likeCount.setSelected(updatedPost.isLikedByCurrentUser);
            showCounts(holder, updatedPost, true);
        }
    }

    /*
     * shows like & comment count
     */
    private void showCounts(ReaderPostViewHolder holder, ReaderPost post, boolean animateChanges) {
        holder.likeCount.setCount(post.numLikes, animateChanges);

        if (post.numReplies > 0 || post.isCommentsOpen) {
            holder.commentCount.setCount(post.numReplies, animateChanges);
            holder.commentCount.setVisibility(View.VISIBLE);
        } else {
            holder.commentCount.setVisibility(View.GONE);
        }
    }

    boolean isValidPosition(int position) {
        return (position >= 0 && position < getItemCount());
    }

    public ReaderPost getItem(int position) {
        if (isValidPosition(position)) {
            return mPosts.get(position);
        } else {
            return null;
        }
    }

    /*
     * triggered when user taps the follow button
     */
    private void toggleFollow(final ReaderFollowButton followButton, int position) {
        ReaderPost post = getItem(position);
        if(post == null)
        {
            return;
        }

        final boolean isAskingToFollow = !post.isFollowedByCurrentUser;
        followButton.setIsFollowedAnimated(isAskingToFollow);

        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {

            }
        };

        if (ReaderBlogActions.followBlogForPost(post, isAskingToFollow, actionListener)) {
            ReaderPost updatedPost = ReaderPostTable.getPost(post.blogId, post.postId, true);
            if (updatedPost != null) {
                mPosts.set(position, updatedPost);
                copyBlogFollowStatus(updatedPost);
            }
        }
    }

    public boolean isEmpty() {
        return (getItemCount() == 0);
    }
    /*
     * copy the follow status from the passed post to other posts in the same blog
     */
    private void copyBlogFollowStatus(final ReaderPost post) {
        if (isEmpty() || post == null) {
            return;
        }

        long blogId = post.blogId;
        String blogUrl = post.getBlogUrl();

        boolean hasBlogId = (blogId != 0);
        boolean hasBlogUrl = !TextUtils.isEmpty(blogUrl);
        if (!hasBlogId && !hasBlogUrl) {
            return;
        }

        long skipPostId = post.postId;
        boolean followStatus = post.isFollowedByCurrentUser;
        boolean isMatched;

        for (ReaderPost thisPost : mPosts) {
            if (hasBlogId) {
                isMatched = (blogId == thisPost.blogId && skipPostId != thisPost.postId);
            } else {
                isMatched = blogUrl.equals(thisPost.getBlogUrl());
            }
            if (isMatched && thisPost.isFollowedByCurrentUser != followStatus) {
                thisPost.isFollowedByCurrentUser = followStatus;
                int position = mPosts.indexOfPost(thisPost);
                if (position > -1) {
                    notifyItemChanged(position);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }
}


