package com.example.manish.androidcms.ui.reader;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toolbar;

import com.cocosw.undobar.UndoBarController;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.datasets.ReaderBlogTable;
import com.example.manish.androidcms.datasets.ReaderDatabase;
import com.example.manish.androidcms.datasets.ReaderPostTable;
import com.example.manish.androidcms.datasets.ReaderTagTable;
import com.example.manish.androidcms.models.ReaderPost;
import com.example.manish.androidcms.models.ReaderPostList;
import com.example.manish.androidcms.models.ReaderTag;
import com.example.manish.androidcms.models.ReaderTagType;
import com.example.manish.androidcms.ui.prefs.AppPrefs;
import com.example.manish.androidcms.ui.reader.actions.ReaderActions;
import com.example.manish.androidcms.ui.reader.actions.ReaderUserActions;
import com.example.manish.androidcms.ui.reader.adapters.ReaderPostAdapter;
import com.example.manish.androidcms.ui.reader.adapters.ReaderTagSpinnerAdapter;
import com.example.manish.androidcms.ui.reader.services.ReaderPostService;
import com.example.manish.androidcms.ui.reader.views.ReaderBlogInfoView;
import com.example.manish.androidcms.ui.reader.views.ReaderRecyclerView;
import com.example.manish.androidcms.util.AniUtils;
import com.example.manish.androidcms.util.CMSActivityUtils;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import de.greenrobot.event.EventBus;

/**
 * Created by Manish on 12/29/2015.
 */
public class ReaderPostListFragment extends Fragment
        implements ReaderInterfaces.OnPostSelectedListener,
        ReaderInterfaces.OnTagSelectedListener,
        ReaderInterfaces.OnPostPopupListener{

    //--Fragment Lifecycle

    //1.At very beginning of the fragment life the method onInflate is called.
    // We have to notice that this method is called only if we define fragment
    // directly in our layout using the tag <fragment>. In this method we
    // can save some configuration parameter and some attributes define in the
    // XML layout file.

    //2.  After this step onAttach is called. This method is called as soon as the
    // fragment is “attached” to
    // the “father” activity and we can this method to store the reference about
    // the activity.

    /*
     * called from adapter when user taps a post
     */
    @Override
    public void onPostSelected(long blogId, long postId) {

    }

    /*
     * called from adapter when user taps a tag on a post
     */
    @Override
    public void onTagSelected(String tagName) {

    }

    private boolean isCurrentTagName(String tagName) {
        return (tagName != null && tagName.equalsIgnoreCase(getCurrentTagName()));
    }
    /*
    * when previewing posts with a specific tag, a history of previewed tags is retained so
    * the user can navigate back through them - this is faster and requires less memory
    * than creating a new fragment for each previewed tag
    */
    boolean goBackInTagHistory() {
        if (mTagPreviewHistory.empty()) {
            return false;
        }

        String tagName = mTagPreviewHistory.pop();
        if (isCurrentTagName(tagName)) {
            if (mTagPreviewHistory.empty()) {
                return false;
            }
            tagName = mTagPreviewHistory.pop();
        }

        setCurrentTag(new ReaderTag(tagName, ReaderTagType.FOLLOWED), false);
        updateFollowButton();

        return true;
    }

    /*
     * called when user taps dropdown arrow icon next to a post - shows a popup menu
     * that enables blocking the blog the post is in
     */
    @Override
    public void onShowPostPopup(View view, final ReaderPost post) {

    }
    private ReaderRecyclerView mRecyclerView;
    private final HistoryStack mTagPreviewHistory = new HistoryStack("tag_preview_history");

    private static class HistoryStack extends Stack<String> {
        private final String keyName;
        HistoryStack(String keyName) {
            this.keyName = keyName;
        }
        void restoreInstance(Bundle bundle) {
            clear();
            if (bundle.containsKey(keyName)) {
                ArrayList<String> history = bundle.getStringArrayList(keyName);
                if (history != null) {
                    this.addAll(history);
                }
            }
        }
        void saveInstance(Bundle bundle) {
            if (!isEmpty()) {
                ArrayList<String> history = new ArrayList<>();
                history.addAll(this);
                bundle.putStringArrayList(keyName, history);
            }
        }
    }

    private Spinner mSpinner;
    private ViewGroup mTagInfoView;
    private ReaderTagSpinnerAdapter mSpinnerAdapter;
    private ReaderBlogInfoView mBlogInfoView;
    private View mEmptyView;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;

    private ReaderTypes.ReaderPostListType mPostListType;

    private ReaderTag mCurrentTag;
    private int mRestorePosition;

    private ProgressBar mProgress;
    private boolean mWasPaused;

    /*
     * box/pages animation that appears when loading an empty list (only appears for tags)
     */
    private boolean shouldShowBoxAndPagesAnimation() {
        return getPostListType().isTagType();
    }

    private View mNewPostsBar;
    /*
     * show posts with a specific tag (either TAG_FOLLOWED or TAG_PREVIEW)
     */
    static ReaderPostListFragment newInstanceForTag(ReaderTag tag, ReaderTypes.ReaderPostListType
                                                    listType)
    {

        AppLog.d(AppLog.T.READER, "reader post list > newInstance (tag)");

        Bundle args = new Bundle();
        args.putSerializable(ReaderConstants.ARG_TAG, tag);
        args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, listType);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);

        return fragment;
    }


    /*
     * show posts in a specific blog
     */
    public static ReaderPostListFragment newInstanceForBlog(long blogId) {
        AppLog.d(AppLog.T.READER, "reader post list > newInstance (blog)");

        Bundle args = new Bundle();
        args.putLong(ReaderConstants.ARG_BLOG_ID, blogId);
        args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, ReaderTypes.ReaderPostListType.BLOG_PREVIEW);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);

        return fragment;
    }
    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        if (args != null) {
            if (args.containsKey(ReaderConstants.ARG_TAG)) {
                mCurrentTag = (ReaderTag) args.getSerializable(ReaderConstants.ARG_TAG);
            }
            /*if (args.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) args.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }

            mCurrentBlogId = args.getLong(ReaderConstants.ARG_BLOG_ID);
            mCurrentFeedId = args.getLong(ReaderConstants.ARG_FEED_ID);

            if (getPostListType() == ReaderPostListType.TAG_PREVIEW && hasCurrentTag()) {
                mTagPreviewHistory.push(getCurrentTagName());
            }*/
        }
    }

    //3. After it we have onCreate. It is one of the most important step, our fragment is
    // in the creation process. This method can
    //be used to start some thread to retrieve data information, maybe from a remote server.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null)
        {
            AppLog.d(AppLog.T.READER, "reader post list > restoring instance state");
            if(savedInstanceState.containsKey(ReaderConstants.ARG_TAG))
            {
                mCurrentTag = (ReaderTag) savedInstanceState.getSerializable(ReaderConstants.ARG_TAG);
            }

            mRestorePosition = savedInstanceState.getInt(ReaderConstants.KEY_RESTORE_POSITION);
            mWasPaused = savedInstanceState.getBoolean(ReaderConstants.KEY_WAS_PAUSED);
        }

    }

    //4.The onCreateView is the method called when the fragment has to create its view
    // hierarchy. During this method we will inflate our layout inside the fragment as
    // we do for example in the ListView widget. During this phase we can’t be
    // sure that our activity is still created so we can’t count on it for some operation.
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.reader_fragment_post_cards,
                container, false);

        mRecyclerView = (ReaderRecyclerView) rootView.findViewById(R.id.recycler_view);

        Context context = container.getContext();
        int spacingHorizontal = context.getResources().getDimensionPixelSize(R.dimen.reader_card_spacing);
        int spacingVertical = context.getResources().getDimensionPixelSize(R.dimen.reader_card_gutters);

        mRecyclerView.addItemDecoration(new ReaderRecyclerView.ReaderItemDecoration(spacingHorizontal,
                spacingVertical));

        // bar that appears at top after new posts are loaded

        mNewPostsBar = rootView.findViewById(R.id.layout_new_posts);
        mNewPostsBar.setVisibility(View.GONE);
        mNewPostsBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideNewPostsBar();
                mRecyclerView.scrollToPosition(0);
            }
        });

        // add the tag/blog header - note that this remains invisible until animated in
        ViewGroup header = (ViewGroup) rootView.findViewById(R.id.frame_header);
        switch (getPostListType())
        {
            case TAG_PREVIEW:
             mTagInfoView = (ViewGroup)
                     inflater.inflate(R.layout.reader_tag_info_view, container, false);

                header.addView(mTagInfoView);
                header.setVisibility(View.INVISIBLE);
                break;

            case BLOG_PREVIEW:
                mBlogInfoView = new ReaderBlogInfoView(context);
                header.addView(mBlogInfoView);
                header.setVisibility(View.INVISIBLE);
                break;


        }

        // view that appears when current tag/blog has no posts - box images in this view are
        // displayed and animated for tags only
        mEmptyView = rootView.findViewById(R.id.empty_view);

        mEmptyView.findViewById(R.id.layout_box_images).
                setVisibility(shouldShowBoxAndPagesAnimation() ? View.VISIBLE : View.GONE);

        // progress bar that appears when loading more posts
        mProgress = (ProgressBar) rootView.findViewById(R.id.progress_footer);
        mProgress.setVisibility(View.GONE);

        // swipe to refresh setup
        // swipe to refresh setup
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(getActivity(),
                (CustomSwipeRefreshLayout) rootView.findViewById(R.id.ptr_layout),
                new SwipeToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!isAdded()) {
                            return;
                        }
                        if (!NetworkUtils.checkConnection(getActivity())) {
                            showSwipeToRefreshProgress(false);
                            return;
                        }
                        switch (getPostListType()) {
                            case TAG_FOLLOWED:
                            case TAG_PREVIEW:
                                updatePostsWithTag(getCurrentTag(), ReaderPostService.UpdateAction.REQUEST_NEWER);
                                break;
                            case BLOG_PREVIEW:
                                //updatePostsInCurrentBlogOrFeed(ReaderPostService.UpdateAction.REQUEST_NEWER);
                                break;
                        }
                        // make sure swipe-to-refresh progress shows since this is a manual refresh
                        showSwipeToRefreshProgress(true);
                    }
                }
        );

        return rootView;
    }


    //5. We get notified when the “father” activity is created and ready in the onActivityCreated.
    // From now on, our activity is active and created and we can use it when we need.
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);
        setupActionBar();

        boolean adapterAlreadyExists = hasPostAdapter();
        mRecyclerView.setAdapter(getPostAdapter());

        // if adapter didn't already exist, populate it now then update the tag - this
        // check is important since without it the adapter would be reset and posts would
        // be updated every time the user moves between fragments
        if(!adapterAlreadyExists && getPostListType().isTagType())
        {
            boolean isRecreated = (savedInstanceState != null);
            getPostAdapter().setCurrentTag(mCurrentTag);
            if(!isRecreated && ReaderTagTable.shouldAutoUpdateTag(mCurrentTag))
            {
                updatePostsWithTag(getCurrentTag(), ReaderPostService.UpdateAction.REQUEST_NEWER);
            }
        }

        if (getPostListType().isPreviewType()) {
            //createFollowButton();
        }

        switch (getPostListType()) {
            case BLOG_PREVIEW:
               // loadBlogOrFeedInfo();
                //animateHeaderDelayed();
                break;
            case TAG_PREVIEW:
                updateTagPreviewHeader();
                //animateHeaderDelayed();
                break;
        }
    }

    //6. The next step is onStart method. Here we do the common things as in the activity onStart,
    // during this phase our fragment is visible but it isn’t still interacting with the user.

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);

        purgeDatabaseIfNeeded();
        performInitialUpdateIfNeeded();
        if (getPostListType() == ReaderTypes.ReaderPostListType.TAG_FOLLOWED) {
            //updateFollowedTagsAndBlogsIfNeeded();
        }
    }



    /*
     * initial update performed the first time the user opens the reader
     */
    private void performInitialUpdateIfNeeded() {
        if (EventBus.getDefault().getStickyEvent(ReaderEvents.HasPerformedInitialUpdate.class) == null
                && NetworkUtils.isNetworkAvailable(getActivity())) {
            // update current user to ensure we have their user_id as well as their latest info
            // in case they changed their avatar, name, etc. since last time
            AppLog.d(AppLog.T.READER, "reader post list > updating current user");
            ReaderUserActions.updateCurrentUser();

            EventBus.getDefault().postSticky(new ReaderEvents.HasPerformedInitialUpdate());
        }
    }

    /*
     * purge reader db if it hasn't been done yet, but only if there's an active connection
     * since we don't want to purge posts that the user would expect to see when offline
     */
    private void purgeDatabaseIfNeeded() {
        if (EventBus.getDefault().getStickyEvent(ReaderEvents.HasPurgedDatabase.class) == null
                && NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.d(AppLog.T.READER, "reader post list > purging database");
            ReaderDatabase.purgeAsync();
            EventBus.getDefault().postSticky(new ReaderEvents.HasPurgedDatabase());
        }
    }


    //7.When the fragment is ready to interact with user onResume is called.
    // At the end of this phase our fragment is up and running!!

    @Override
    public void onResume() {

        super.onResume();

        if(mWasPaused)
        {
            AppLog.d(AppLog.T.READER, "reader post list > resumed from paused state");
            mWasPaused = false;

            // refresh the posts in case the user returned from an activity that
            // changed one (or more) of the posts
            refreshPosts();
            // likewise for tags
            refreshTags();

            // auto-update the current tag if it's time
            if (!isUpdating()
                    && getPostListType() == ReaderTypes.ReaderPostListType.TAG_FOLLOWED
                    && ReaderTagTable.shouldAutoUpdateTag(mCurrentTag)) {
                AppLog.i(AppLog.T.READER, "reader post list > auto-updating current tag after resume");
                updatePostsWithTag(getCurrentTag(), ReaderPostService.UpdateAction.REQUEST_NEWER);
            }
        }
    }


    //8.Then it can happen that the activity is paused and so the activity’s onPause
    // is called. Well onPause fragment method is called too.
    @Override
    public void onPause() {
        super.onPause();
        mWasPaused = true;
    }

    //9. After it it can happen that the OS decides to destroy our
    // fragment view and so onDestroyView is called.


    //10.After it, if the system decides to dismiss our fragment it calls onDestroy method. Here we should release
    // all the connection active and so on because our fragment is close to die.

    //11.  Even if it is during the destroy phase it is still attached to the father activity.
    // The last step is detach the fragment from the activity and it happens when onDetach is called.


    /*
    * make sure current tag still exists, reset to default if it doesn't
    */
    private void checkCurrentTag() {
        if (hasCurrentTag()
                && getPostListType().equals(ReaderTypes.ReaderPostListType.TAG_FOLLOWED)
                && !ReaderTagTable.tagExists(getCurrentTag())) {
            mCurrentTag = ReaderTag.getDefaultTag();
        }
    }

    /*
     * refresh the list of tags shown in the toolbar spinner
     */
    void refreshTags() {
        if (!isAdded()) {
            return;
        }
        checkCurrentTag();
        if (hasSpinnerAdapter()) {
            getSpinnerAdapter().refreshTags();
        }
    }

    /*
     * refresh adapter so latest posts appear
     */
    void refreshPosts() {
        if (hasPostAdapter()) {
            getPostAdapter().refresh();
        }
    }

    /*
     * ensures that the toolbar is correctly configured based on the type of list
     */
    private void setupActionBar() {
        if (!isAdded() || !(getActivity() instanceof ActionBarActivity)) {
            return;
        }
        final android.support.v7.app.ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        if (actionBar == null) {
            return;
        }

        if (getPostListType().equals(ReaderTypes.ReaderPostListType.TAG_FOLLOWED)) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            if (mSpinner == null) {
                setupSpinner();
            }
            selectTagInSpinner(getCurrentTag());
        } else {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private boolean isCurrentTag(final ReaderTag tag) {
        return ReaderTag.isSameTag(tag, mCurrentTag);
    }

    private void setupSpinner()
    {
        if(!isAdded()) return;

        final Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if(toolbar == null)
        {
            return;
        }

        View view = View.inflate(getActivity(), R.layout.reader_spinner, toolbar);
        mSpinner = (Spinner)view.findViewById(R.id.action_bar_spinner);
        mSpinner.setAdapter(getSpinnerAdapter());
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final ReaderTag tag = (ReaderTag) getSpinnerAdapter().getItem(position);
                if (tag == null) {
                    return;
                }
                if (!isCurrentTag(tag)) {
                    Map<String, String> properties = new HashMap<>();
                    properties.put("tag", tag.getTagName());
                    AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LOADED_TAG, properties);
                    if (tag.isFreshlyPressed()) {
                        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LOADED_FRESHLY_PRESSED);
                    }
                }
                setCurrentTag(tag, true);
                AppLog.d(AppLog.T.READER, String.format("reader post list > tag %s displayed",
                        tag.getTagNameForLog()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nop
            }
        });
    }

    public static ReaderPostListFragment newInstanceForFeed(long feedId) {
        AppLog.d(AppLog.T.READER, "reader post list > newInstance (blog)");

        Bundle args = new Bundle();
        args.putLong(ReaderConstants.ARG_FEED_ID, feedId);
        args.putLong(ReaderConstants.ARG_BLOG_ID, feedId);
        args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, ReaderTypes.ReaderPostListType.BLOG_PREVIEW);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);

        return fragment;
    }

    private ReaderPostAdapter mPostAdapter;

    private boolean hasPostAdapter() {
        return (mPostAdapter != null);
    }

    void setCurrentTag(final ReaderTag tag, boolean allowAutoUpdate) {
        if (tag == null) {
            return;
        }

        // skip if this is already the current tag and the post adapter is already showing it - this
        // will happen when the list fragment is restored and the current tag is re-selected in the
        // toolbar dropdown
        if (isCurrentTag(tag)
                && hasPostAdapter()
                && getPostAdapter().isCurrentTag(tag)) {
            return;
        }

        mCurrentTag = tag;

        switch (getPostListType()) {
            case TAG_FOLLOWED:
                // remember this as the current tag if viewing followed tag
                AppPrefs.setReaderTag(tag);
                break;
            case TAG_PREVIEW:
                mTagPreviewHistory.push(tag.getTagName());
                break;
        }

        getPostAdapter().setCurrentTag(tag);
        hideNewPostsBar();
        hideUndoBar();
        showLoadingProgress(false);

        if (getPostListType() == ReaderTypes.ReaderPostListType.TAG_PREVIEW) {
            updateTagPreviewHeader();
            updateFollowButton();
        }

        // update posts in this tag if it's time to do so
        if (allowAutoUpdate && ReaderTagTable.shouldAutoUpdateTag(tag)) {
            updatePostsWithTag(tag, ReaderPostService.UpdateAction.REQUEST_NEWER);
        }
    }



    private void updateFollowButton() {
        /*if (!isAdded() || mFollowButton == null) {
            return;
        }
        boolean isFollowing;
        switch (getPostListType()) {
            case BLOG_PREVIEW:
                if (mCurrentFeedId != 0) {
                    isFollowing = ReaderBlogTable.isFollowedFeed(mCurrentFeedId);
                } else {
                    isFollowing = ReaderBlogTable.isFollowedBlog(mCurrentBlogId);
                }
                break;
            default:
                isFollowing = ReaderTagTable.isFollowedTagName(getCurrentTagName());
                break;
        }
        mFollowButton.setIsFollowed(isFollowing);*/
    }

    String getCurrentTagName() {
        return (mCurrentTag != null ? mCurrentTag.getTagName() : "");
    }

    /*
     * if we're previewing a tag, show the current tag name in the header and update the
     * follow button to show the correct follow state for the tag
     */
    private void updateTagPreviewHeader() {
        if (mTagInfoView == null) {
            return;
        }

        final TextView txtTagName = (TextView) mTagInfoView.findViewById(R.id.text_tag_name);
        String color = HtmlUtils.colorResToHtmlColor(getActivity(), R.color.white);
        String htmlTag = "<font color=" + color + ">" + getCurrentTagName() + "</font>";
        String htmlLabel = getString(R.string.reader_label_tag_preview, htmlTag);
        txtTagName.setText(Html.fromHtml(htmlLabel));
    }

    /*
    * show/hide progress bar which appears at the bottom of the activity when loading more posts
    */
    private void showLoadingProgress(boolean showProgress) {
        if (isAdded() && mProgress != null) {
            if (showProgress) {
                mProgress.bringToFront();
                mProgress.setVisibility(View.VISIBLE);
            } else {
                mProgress.setVisibility(View.GONE);
            }
        }
    }

    private void hideUndoBar() {
        if (isAdded()) {
            UndoBarController.clear(getActivity());
        }
    }

    private long mCurrentFeedId;
    private long mCurrentBlogId;

    /*
     * called by post adapter to load older posts when user scrolls to the last post
     */
    private final ReaderActions.DataRequestedListener mDataRequestedListener

            = new ReaderActions.DataRequestedListener() {

        @Override
        public void onRequestData() {
            // skip if update is already in progress
            if (isUpdating()) {
                return;
            }

            // request older posts unless we already have the max # to show
            switch (getPostListType())
            {
                case TAG_FOLLOWED:
                case TAG_PREVIEW:
                    if(ReaderPostTable.getNumPostsWithTag(mCurrentTag) < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY)
                    {
                        // request older posts
                        updatePostsWithTag(getCurrentTag(), ReaderPostService.UpdateAction.REQUEST_OLDER);
                        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL);
                    }
                    break;
                case BLOG_PREVIEW:
                    int numPosts;
                    if (mCurrentFeedId != 0) {
                        numPosts = ReaderPostTable.getNumPostsInFeed(mCurrentFeedId);
                    } else {
                        numPosts = ReaderPostTable.getNumPostsInBlog(mCurrentBlogId);
                    }
                    if (numPosts < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                        updatePostsInCurrentBlogOrFeed(ReaderPostService.UpdateAction.REQUEST_OLDER);
                        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL);
                    }
                    break;
            }
        }

    };

    /*
     * get posts for the current blog from the server
     */
    void updatePostsInCurrentBlogOrFeed(final ReaderPostService.UpdateAction updateAction) {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.i(AppLog.T.READER, "reader post list > network unavailable, canceled blog update");
            return;
        }
        if (mCurrentFeedId != 0) {
            ReaderPostService.startServiceForFeed(getActivity(), mCurrentFeedId, updateAction);
        } else {
            //ReaderPostService.startServiceForBlog(getActivity(), mCurrentBlogId, updateAction);
        }
    }

    /*
     * called by post adapter when data has been loaded
     */
    private final ReaderInterfaces.DataLoadedListener mDataLoadedListener =
            new ReaderInterfaces.DataLoadedListener()
            {
                @Override
                public void onDataLoaded(boolean isEmpty)
                {
                    if (!isAdded()) {
                        return;
                    }
                    if (isEmpty) {
                        setEmptyTitleAndDescription(false);
                        mEmptyView.setVisibility(View.VISIBLE);
                        if (shouldShowBoxAndPagesAnimation()) {
                            startBoxAndPagesAnimation();
                        }
                    } else {
                        mEmptyView.setVisibility(View.GONE);
                        if (mRestorePosition > 0) {
                            mRecyclerView.scrollToPosition(mRestorePosition);
                        }
                    }
                    mRestorePosition = 0;
                }
            };

    private void startBoxAndPagesAnimation() {
        if (!isAdded()) {
            return;
        }

        ImageView page1 = (ImageView) mEmptyView.findViewById(R.id.empty_tags_box_page1);
        ImageView page2 = (ImageView) mEmptyView.findViewById(R.id.empty_tags_box_page2);
        ImageView page3 = (ImageView) mEmptyView.findViewById(R.id.empty_tags_box_page3);

        page1.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.box_with_pages_slide_up_page1));
        page2.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.box_with_pages_slide_up_page2));
        page3.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.box_with_pages_slide_up_page3));
    }

    private boolean mIsUpdating;

    boolean isUpdating() {
        return mIsUpdating;
    }

    boolean hasCurrentTag() {
        return mCurrentTag != null;
    }

    private void setEmptyTitleAndDescription(boolean requestFailed) {
        if (!isAdded()) {
            return;
        }

        int titleResId;
        int descriptionResId = 0;

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            titleResId = R.string.reader_empty_posts_no_connection;
        } else if (requestFailed) {
            titleResId = R.string.reader_empty_posts_request_failed;
        } else if (isUpdating()) {
            titleResId = R.string.reader_empty_posts_in_tag_updating;
        } else if (getPostListType() == ReaderTypes.ReaderPostListType.BLOG_PREVIEW) {
            titleResId = R.string.reader_empty_posts_in_blog;
        } else if (getPostListType() == ReaderTypes.ReaderPostListType.TAG_FOLLOWED &&
                hasCurrentTag()) {
            if (getCurrentTag().isBlogsIFollow()) {
                titleResId = R.string.reader_empty_followed_blogs_title;
                descriptionResId = R.string.reader_empty_followed_blogs_description;
            } else if (getCurrentTag().isPostsILike()) {
                titleResId = R.string.reader_empty_posts_liked;
            } else {
                titleResId = R.string.reader_empty_posts_in_tag;
            }
        } else {
            titleResId = R.string.reader_empty_posts_in_tag;
        }

        TextView titleView = (TextView) mEmptyView.findViewById(R.id.title_empty);
        titleView.setText(getString(titleResId));

        TextView descriptionView = (TextView) mEmptyView.findViewById(R.id.description_empty);
        if (descriptionResId == 0) {
            descriptionView.setVisibility(View.INVISIBLE);
        } else {
            descriptionView.setText(getString(descriptionResId));
            descriptionView.setVisibility(View.VISIBLE);
        }
    }





    private final ReaderPostList mPosts = new ReaderPostList();


    private ReaderPostAdapter getPostAdapter()
    {
        if (mPostAdapter == null) {
            AppLog.d(AppLog.T.READER, "reader post list > creating post adapter");
            Context context = CMSActivityUtils.getThemedContext(getActivity());
            mPostAdapter = new ReaderPostAdapter(context, getPostListType());
            mPostAdapter.setOnPostSelectedListener(this);
            mPostAdapter.setOnTagSelectedListener(this);
            mPostAdapter.setOnPostPopupListener(this);
            mPostAdapter.setOnDataLoadedListener(mDataLoadedListener);
            mPostAdapter.setOnDataRequestedListener(mDataRequestedListener);
            //mPostAdapter.setOnReblogRequestedListener(mRequestReblogListener);

        }
        return mPostAdapter;
    }

    private boolean hasSpinnerAdapter() {
        return (mSpinnerAdapter != null);
    }
    /*
     * make sure the passed tag is the one selected in the spinner
     */
    private void selectTagInSpinner(final ReaderTag tag) {
        if (mSpinner == null || !hasSpinnerAdapter()) {
            return;
        }
        int position = getSpinnerAdapter().getIndexOfTag(tag);
        if (position > -1 && position != mSpinner.getSelectedItemPosition()) {
            mSpinner.setSelection(position);
        }
    }

    /*
     * toolbar spinner adapter which shows list of tags
     */
    private ReaderTagSpinnerAdapter getSpinnerAdapter()
    {
        if(mSpinnerAdapter == null) {
            AppLog.d(AppLog.T.READER, "reader post list > creating spinner adapter");
            ReaderInterfaces.DataLoadedListener dataListener =
                    new ReaderInterfaces.DataLoadedListener() {
                        @Override
                        public void onDataLoaded(boolean isEmpty) {
                            if(isAdded())
                            {
                                AppLog.d(AppLog.T.READER, "reader post list > spinner adapter loaded");
                                selectTagInSpinner(getCurrentTag());
                            }
                        }
                    };

            mSpinnerAdapter = new ReaderTagSpinnerAdapter(getActivity(), dataListener);
        }

        return mSpinnerAdapter;
    }

    ReaderTag getCurrentTag() {
        return mCurrentTag;
    }

    /*
     * get latest posts for this tag from the server
     */
    void updatePostsWithTag(ReaderTag tag, ReaderPostService.UpdateAction updateAction) {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.i(AppLog.T.READER, "reader post list > network unavailable, canceled tag update");
            return;
        }
        ReaderPostService.startServiceForTag(getActivity(), tag, updateAction);
    }

    private void showSwipeToRefreshProgress(boolean showProgress) {
        if (mSwipeToRefreshHelper != null && mSwipeToRefreshHelper.isRefreshing() != showProgress) {
            mSwipeToRefreshHelper.setRefreshing(showProgress);
        }
    }

    /*
     * are we showing all posts with a specific tag (followed or previewed), or all
     * posts in a specific blog?
     */
    ReaderTypes.ReaderPostListType getPostListType() {
        return (mPostListType != null ? mPostListType : ReaderTypes.DEFAULT_POST_LIST_TYPE);
    }

    private boolean mIsAnimatingOutNewPostsBar;

    private void hideNewPostsBar() {
        if (!isAdded() || !isNewPostsBarShowing() || mIsAnimatingOutNewPostsBar) {
            return;
        }

        mIsAnimatingOutNewPostsBar = true;

        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                mNewPostsBar.setVisibility(View.GONE);
                mIsAnimatingOutNewPostsBar = false;
            }
            @Override
            public void onAnimationRepeat(Animation animation) { }
        };
        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_out, listener);
    }

    /*
     * bar that appears at the top when new posts have been retrieved
     */
    private boolean isNewPostsBarShowing() {
        return (mNewPostsBar != null && mNewPostsBar.getVisibility() == View.VISIBLE);
    }
}
