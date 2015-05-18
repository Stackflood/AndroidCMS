package com.example.manish.androidcms.ui.posts;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.Constants;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.Blog;
import com.example.manish.androidcms.models.Post;
import com.example.manish.androidcms.ui.ActivityId;
import com.example.manish.androidcms.util.WPHtml;
import com.example.manish.androidcms.widgets.WPViewPager;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.editor.EditorFragmentAbstract;
import org.wordpress.android.editor.LegacyEditorFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;

import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;

/**
 * Created by Manish on 5/11/2015.
 */
public class EditPostActivity extends ActionBarActivity
        implements EditorFragmentAbstract.EditorFragmentListener {


    public static final String EXTRA_IS_NEW_POST = "isNewPost";
    public static final String EXTRA_IS_QUICKPRESS = "isQuickPress";
    public static final String EXTRA_QUICKPRESS_BLOG_ID = "quickPressBlogId";
    public static final String STATE_KEY_CURRENT_POST = "stateKeyCurrentPost";
    public static final String STATE_KEY_ORIGINAL_POST = "stateKeyOriginalPost";

    public static final String EXTRA_POSTID = "postId";
    public static final String EXTRA_IS_PAGE = "isPage";

    // Moved from EditPostContentFragment
    public static final String NEW_MEDIA_GALLERY = "NEW_MEDIA_GALLERY";
    public static final String NEW_MEDIA_GALLERY_EXTRA_IDS = "NEW_MEDIA_GALLERY_EXTRA_IDS";
    public static final String NEW_MEDIA_POST = "NEW_MEDIA_POST";
    public static final String NEW_MEDIA_POST_EXTRA = "NEW_MEDIA_POST_ID";
    private String mMediaCapturePath = "";
    private int mMaxThumbWidth = 0;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    private static int PAGE_CONTENT = 0;
    private static int PAGE_SETTINGS = 1;
    private static int PAGE_PREVIEW = 2;
    private Post mPost;
    private boolean mIsNewPost;
    private Post mOriginalPost;

    private static final int AUTOSAVE_INTERVAL_MILLIS = 10000;

    /**
     * The {@link android.support.v4.view.ViewPager} that will host the section contents.
     */
    WPViewPager mViewPager;

    private EditorFragmentAbstract mEditorFragment;

    private EditPostSettingsFragment mEditPostSettingsFragment;
    private EditPostPreviewFragment mEditPostPreviewFragment;

    private Timer mAutoSaveTimer;

    public Post getPost() {
        return mPost;
    }

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_edit_post);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0.0f);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Bundle extras = getIntent().getExtras();
        String action = getIntent().getAction();
        if (savedInstanceState == null) {
            if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)
                    || NEW_MEDIA_GALLERY.equals(action)
                    || NEW_MEDIA_POST.equals(action)
                    || getIntent().hasExtra(EXTRA_IS_QUICKPRESS)
                    || (extras != null && extras.getInt("quick-media", -1) > -1)) {
                if (getIntent().hasExtra(EXTRA_QUICKPRESS_BLOG_ID)) {
                    // QuickPress might want to use a different blog than the current blog
                    int blogId = getIntent().getIntExtra(EXTRA_QUICKPRESS_BLOG_ID, -1);
                    Blog quickPressBlog = CMS.cmsDB.instantiateBlogByLocalId(blogId);
                    if (quickPressBlog == null) {
                        showErrorAndFinish(R.string.blog_not_found);
                        return;
                    }
                    if (quickPressBlog.isHidden()) {
                        showErrorAndFinish(R.string.error_blog_hidden);
                        return;
                    }
                    CMS.currentBlog = quickPressBlog;
                }

                // Create a new post for share intents and QuickPress
                mPost = new Post(CMS.getCurrentLocalTableBlogId(), false);
                //CMS.cmsDB.savePost(mPost);
                mIsNewPost = true;
            } else if (extras != null) {
                // Load post from the postId passed in extras
                long localTablePostId = extras.getLong(EXTRA_POSTID, -1);
                boolean isPage = extras.getBoolean(EXTRA_IS_PAGE);
                mIsNewPost = extras.getBoolean(EXTRA_IS_NEW_POST);
                mPost = CMS.cmsDB.getPostForLocalTablePostId(localTablePostId);
                mOriginalPost = CMS.cmsDB.getPostForLocalTablePostId(localTablePostId);
            } else {
                // A postId extra must be passed to this activity
                showErrorAndFinish(R.string.post_not_found);
                return;
            }
        }
        else if (savedInstanceState.containsKey(STATE_KEY_ORIGINAL_POST)) {
            try {
                mPost = (Post) savedInstanceState.getSerializable(STATE_KEY_CURRENT_POST);
                mOriginalPost = (Post) savedInstanceState.getSerializable(STATE_KEY_ORIGINAL_POST);
            } catch (ClassCastException e) {
                mPost = null;
            }
        }

        // Ensure we have a valid blog
        if (CMS.getCurrentBlog() == null) {
            showErrorAndFinish(R.string.blog_not_found);
            return;
        }

        // Ensure we have a valid post
        if (mPost == null) {
            showErrorAndFinish(R.string.post_not_found);
            return;
        }

        if (mIsNewPost) {
            //trackEditorCreatedPost(action, getIntent());
        }

        setTitle(StringUtils.unescapeHTML(CMS.getCurrentBlog().getBlogName()));

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (WPViewPager)findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setPagingEnabled(false);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
        {
            @Override
            public void onPageSelected(int position) {
                invalidateOptionsMenu();
                if (position == PAGE_CONTENT) {
                    setTitle(StringUtils.unescapeHTML(CMS.getCurrentBlog().getBlogName()));
                } else if (position == PAGE_SETTINGS) {
                    setTitle(mPost.isPage() ? R.string.page_settings : R.string.post_settings);
                } else if (position == PAGE_PREVIEW) {
                    setTitle(mPost.isPage() ? R.string.preview_page : R.string.preview_post);
                    savePost(true);
                    if (mEditPostPreviewFragment != null) {
                        mEditPostPreviewFragment.loadPost();
                    }
                }
            }

        });

        ActivityId.trackLastActivity(ActivityId.POST_EDITOR);

        registerReceiver(mGalleryReceiver,
                new IntentFilter(LegacyEditorFragment.ACTION_MEDIA_GALLERY_TOUCHED));
    }

    private BroadcastReceiver mGalleryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LegacyEditorFragment.ACTION_MEDIA_GALLERY_TOUCHED.equals(intent.getAction())) {
                //startMediaGalleryActivity((MediaGallery)intent.getSerializableExtra(LegacyEditorFragment.EXTRA_MEDIA_GALLERY));
            }
        }
    };

    class AutoSaveTask extends TimerTask {
        public void run() {
            savePost(true);
        }
    }

    private void updatePostObject(boolean isAutosave) {
        if (mPost == null) {
            AppLog.e(AppLog.T.POSTS, "Attempted to save an invalid Post.");
            return;
        }

        // Update post object from fragment fields
        if (mEditorFragment != null) {
            //updatePostContent(isAutosave);
        }
        if (mEditPostSettingsFragment != null) {
            mEditPostSettingsFragment.updatePostSettings();
        }
    }

    private void savePost(boolean isAutosave, boolean updatePost) {
        if (updatePost) {
            updatePostObject(isAutosave);
        }

        CMS.cmsDB.updatePost(mPost);
    }

    private void savePost(boolean isAutosave) {
        savePost(isAutosave, true);
    }

    @Override
    public void onStart() {
        super.onStart();
        //EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //refreshBlogMedia();
        mAutoSaveTimer = new Timer();
        mAutoSaveTimer.scheduleAtFixedRate(new AutoSaveTask(), AUTOSAVE_INTERVAL_MILLIS,
                AUTOSAVE_INTERVAL_MILLIS);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stopMediaUploadService();
        mAutoSaveTimer.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mGalleryReceiver);
      //  AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_CLOSED_POST);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Saves both post objects so we can restore them in onCreate()
        savePost(true);
        outState.putSerializable(STATE_KEY_CURRENT_POST, mPost);
        outState.putSerializable(STATE_KEY_ORIGINAL_POST, mOriginalPost);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_post, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem previewMenuItem = menu.findItem(R.id.menu_preview_post);
        if (mViewPager != null && mViewPager.getCurrentItem() > PAGE_CONTENT) {
            previewMenuItem.setVisible(false);
        } else {
            previewMenuItem.setVisible(true);
        }

        // Set text of the save button in the ActionBar
        if (mPost != null) {
            MenuItem saveMenuItem = menu.findItem(R.id.menu_save_post);
            switch (mPost.getStatusEnum()) {
                case SCHEDULED:
                    saveMenuItem.setTitle(getString(R.string.schedule_verb));
                    break;
                case PUBLISHED:
                case UNKNOWN:
                    if (mPost.isLocalDraft()) {
                        saveMenuItem.setTitle(R.string.publish_post);
                    } else {
                        saveMenuItem.setTitle(R.string.update_verb);
                    }
                    break;
                default:
                    if (mPost.isLocalDraft()) {
                        saveMenuItem.setTitle(R.string.save);
                    } else {
                        saveMenuItem.setTitle(R.string.update_verb);
                    }
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem() > PAGE_CONTENT) {
            mViewPager.setCurrentItem(PAGE_CONTENT);
            invalidateOptionsMenu();
            return;
        }

        if (mEditorFragment != null && !mEditorFragment.onBackPressed()) {
           saveAndFinish();
        }
    }

    /**
     * A {@link android.support.v13.app.FragmentPagerAdapter} that returns a fragment corresponding
     * to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            switch (position) {
                case 0:
                    mEditorFragment = (EditorFragmentAbstract) fragment;
                    break;
                case 1:
                    mEditPostSettingsFragment = (EditPostSettingsFragment) fragment;
                    break;
                case 2:
                    mEditPostPreviewFragment = (EditPostPreviewFragment) fragment;
                    break;
            }
            return fragment;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case 0:
                    // TODO: switch between legacy and new editor here (AB test?)
                    return new LegacyEditorFragment();
                case 1:
                    return new EditPostSettingsFragment();
                default:
                return new EditPostPreviewFragment();
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }

    // Menu actions
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_save_post) {
            // If the post is new and there are no changes, don't publish
            updatePostObject(false);
            if (!mPost.isPublishable()) {
                ToastUtils.showToast(this, R.string.error_publish_empty_post, ToastUtils.Duration.SHORT);
                return false;
            }

            savePost(false, false);
            //trackSavePostAnalytics();

            if (!NetworkUtils.isNetworkAvailable(this)) {
                ToastUtils.showToast(this, R.string.error_publish_no_network,
                        ToastUtils.Duration.SHORT);
                return false;
            }

            //PostUploadService.addPostToUpload(mPost);
            //startService(new Intent(this, PostUploadService.class));
            Intent i = new Intent();
            i.putExtra("shouldRefresh", true);
            setResult(RESULT_OK, i);
            finish();
            return true;
        } else if (itemId == R.id.menu_preview_post) {
            mViewPager.setCurrentItem(PAGE_PREVIEW);
        } else if (itemId == android.R.id.home) {
            if (mViewPager.getCurrentItem() > PAGE_CONTENT) {
                mViewPager.setCurrentItem(PAGE_CONTENT);
                invalidateOptionsMenu();
            } else {
                saveAndFinish();
            }
            return true;
        }
        return false;
    }

    private boolean hasEmptyContentFields() {
        return TextUtils.isEmpty(mEditorFragment.getTitle())
                && TextUtils.isEmpty(mEditorFragment.getContent());
    }

    private void saveAndFinish() {
        savePost(true);
        if (mEditorFragment != null && hasEmptyContentFields()) {
            // new and empty post? delete it
            if (mIsNewPost) {
                CMS.cmsDB.deletePost(mPost);
            }
        } else if (mOriginalPost != null && !mPost.hasChanges(mOriginalPost)) {
            // if no changes have been made to the post, set it back to the original don't save it
            CMS.cmsDB.updatePost(mOriginalPost);
            CMS.currentPost = mOriginalPost;
        } else {
            // changes have been made, save the post and ask for the post list to refresh.
            // We consider this being "manual save", it will replace some Android "spans" by an html
            // or a shortcode replacement (for instance for images and galleries)
            savePost(false);
            CMS.currentPost = mPost;
            Intent i = new Intent();
            i.putExtra("shouldRefresh", true);
            setResult(RESULT_OK, i);
        }
        finish();
    }

    private void showErrorAndFinish(int errorMessageId) {
        Toast.makeText(this, getResources().getText(errorMessageId), Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onSettingsClicked() {
        mViewPager.setCurrentItem(PAGE_SETTINGS);
    }

    private int getMaximumThumbnailWidthForEditor() {
        if (mMaxThumbWidth == 0) {
            mMaxThumbWidth = ImageUtils.getMaximumThumbnailWidthForEditor(this);
        }
        return mMaxThumbWidth;
    }

    private class LoadPostContentTask extends AsyncTask<String, Spanned, Spanned> {
        @Override
        protected Spanned doInBackground(String... params) {
            if (params.length < 1 || getPost() == null) {
                return null;
            }

            String content = StringUtils.notNullStr(params[0]);
            return WPHtml.fromHtml(content, EditPostActivity.this, getPost(), getMaximumThumbnailWidthForEditor());
        }

        @Override
        protected void onPostExecute(Spanned spanned) {
            if (spanned != null) {
                mEditorFragment.setContent(spanned);
            }
        }
    }

    private void fillContentEditorFields() {
        // Needed blog settings needed by the editor
        if (CMS.getCurrentBlog() != null) {
            mEditorFragment.setFeaturedImageSupported(CMS.getCurrentBlog().isFeaturedImageCapable());
            mEditorFragment.setBlogSettingMaxImageWidth(CMS.getCurrentBlog().getMaxImageWidth());
        }

        // Set post title and content
        Post post = getPost();
        if (post != null) {
            if (!TextUtils.isEmpty(post.getContent())) {
                if (post.isLocalDraft()) {
                    // Load local post content in the background, as it may take time to generate images
                    new LoadPostContentTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                            post.getContent().replaceAll("\uFFFC", ""));
                }
                else {
                    mEditorFragment.setContent(post.getContent().replaceAll("\uFFFC", ""));
                }
            }
            if (!TextUtils.isEmpty(post.getTitle())) {
                mEditorFragment.setTitle(post.getTitle());
            }
            // TODO: postSettingsButton.setText(post.isPage() ? R.string.page_settings : R.string.post_settings);
            mEditorFragment.setLocalDraft(post.isLocalDraft());
        }

        // Special actions
        String action = getIntent().getAction();
        /*int quickMediaType = getIntent().getIntExtra("quick-media", -1);
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            setPostContentFromShareAction();
        } else if (NEW_MEDIA_GALLERY.equals(action)) {
            prepareMediaGallery();
        } else if (NEW_MEDIA_POST.equals(action)) {
            prepareMediaPost();
        } else if (quickMediaType >= 0) {
            // User selected 'Quick Photo' in the menu drawer
            if (quickMediaType == Constants.QUICK_POST_PHOTO_CAMERA) {
                launchCamera();
            } else if (quickMediaType == Constants.QUICK_POST_PHOTO_LIBRARY) {
                WordPressMediaUtils.launchPictureLibrary(this);
            }
            if (post != null) {
                post.setQuickPostType(Post.QUICK_MEDIA_TYPE_PHOTO);
            }
        }*/
    }

    @Override
    public void onEditorFragmentInitialized() {
        fillContentEditorFields();
    }

    @Override
    public void saveMediaFile(MediaFile mediaFile) {
        //WordPress.wpDB.saveMediaFile(mediaFile);
    }

    @Override
    public void onAddMediaClicked() {
        //startMediaSelection();
    }
}
