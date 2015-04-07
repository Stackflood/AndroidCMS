package com.example.manish.androidcms.ui;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.ui.accounts.SignInActivity;
import com.example.manish.androidcms.ui.analytics.AnalyticsTracker;
import com.example.manish.androidcms.ui.helpers.ListScrollPositionManager;
import com.example.manish.androidcms.ui.stats.StatsActivity;
import com.example.manish.androidcms.util.CMSActivityUtils;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;

//import de.greenrobot.event.EventBus;

/**
 * Base class for Activities that include a standard action bar and menu drawer.
 */
public abstract class CMSDrawerActivity extends ActionBarActivity {

    private static final String SCROLL_POSITION_ID = "WPDrawerActivity";
    /**
     * AuthenticatorRequest code used when no accounts exist, and user is prompted to add an
     * account.
     */
    private static final int ADD_ACCOUNT_REQUEST = 100;
    /**
     * AuthenticatorRequest code for reloading menu after returning from  the PreferencesActivity.
     */
    private static final int SETTINGS_REQUEST = 200;
    /**
     * AuthenticatorRequest code for re-authentication
     */
    private static final int AUTHENTICATE_REQUEST = 300;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar mToolbar;
    private boolean isAnimatingRefreshButton;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private DrawerAdapter mDrawerAdapter;
    private boolean mShouldFinish;
    private ListScrollPositionManager mScrollPositionManager;

    protected void createMenuDrawer(int contentViewId)
    {
        ViewGroup container = (ViewGroup)findViewById(R.id.activity_container);
        container.addView(getLayoutInflater().inflate(contentViewId, null));
        initMenuDrawer();
    }

    protected ActionBarDrawerToggle getDrawerToggle() {
        return mDrawerToggle;
    }

    /**ActionBarOverlayLayout
     * Create menu drawer ListView and listeners
     */
    private void initMenuDrawer() {
        // locate the drawer layout - note that it will not exist on landscape tablets
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout != null) {
            int drawerWidth =
                    isStaticMenuDrawer() ?
                            getResources().getDimensionPixelSize(R.dimen.drawer_width_static) :
                            CMSActivityUtils.getOptimalDrawerWidth(this);

            ViewGroup leftDrawer = (ViewGroup)mDrawerLayout.findViewById(R.id.capture_insets_frame_layout);
            leftDrawer.getLayoutParams().width = drawerWidth;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.color_primary_dark));
            }
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
            mDrawerToggle = new ActionBarDrawerToggle(
                    this,
                    mDrawerLayout,
                    getToolbar(),
                    R.string.open_drawer,
                    R.string.close_drawer
            ) {
                public void onDrawerClosed(View view) {
                    invalidateOptionsMenu();
                }
                public void onDrawerOpened(View drawerView) {
                    invalidateOptionsMenu();
                }
            };
            mDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }

        // add listVew header containing spinner if it hasn't already been added - note that
        // initBlogSpinner() will setup the spinner
        mDrawerListView = (ListView) findViewById(R.id.drawer_list);
        if(mDrawerListView.getHeaderViewsCount()==0)
        {
            View view = getLayoutInflater().inflate(R.layout.drawer_header,mDrawerListView, false);
            mDrawerListView.addHeaderView(view, null, false);
        }

        mScrollPositionManager = new ListScrollPositionManager(mDrawerListView, false);
        View settingsRow = findViewById(R.id.settings_row);

        /*settingsRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        });*/

        mDrawerAdapter = new DrawerAdapter(this);
        mDrawerListView.setAdapter(mDrawerAdapter);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int menuPosition = position - mDrawerListView.getHeaderViewsCount();
                DrawerItems.DrawerItem item = (DrawerItems.DrawerItem) mDrawerAdapter.getItem(menuPosition);
                drawerItemSelected(item);
            }
        });


        /*initBlogSpinner();*/
        updateMenuDrawer();

        setToolbarClickListener();

    }

    protected void setToolbarClickListener() {
        // Set navigation listener, which ensures menu button works on all devices (#2157)
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFinishing()) return;

                FragmentManager fm = getFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    try {
                        fm.popBackStack();
                    } catch (IllegalStateException e) {
                        // onClick event can be fired after the onSaveInstanceState call,
                        // and make the app crash. Catching this exception avoid the crash. If this existed,
                        // we would use popBackStackAllowingStateLoss.
                    }
                } else if (isStaticMenuDrawer()) {
                    finish();
                } else {
                    toggleDrawer();
                }
            }
        });
    }


    private void openDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }



    private void toggleDrawer() {
        if (mDrawerLayout != null) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                closeDrawer();
            } else {
                openDrawer();
            }
        }
    }

    /**
     * Update all of the items in the menu drawer based on the current active blog.
     */
    public void updateMenuDrawer() {
        mDrawerAdapter.refresh();
    }

    void closeDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }
    /**
     * called when user selects an item from the drawer
     */
    private void drawerItemSelected(DrawerItems.DrawerItem item) {
        // do nothing if item is already selected
        if (item == null || item.isSelected(this)) {
            closeDrawer();
            return;
        }

        // if the item has an activity id, remember it for launch
        ActivityId activityId = item.getDrawerItemId().toActivityId();
        if (activityId != ActivityId.UNKNOWN) {
            ActivityId.trackLastActivity(activityId);
        }

        final Intent intent;
        switch (item.getDrawerItemId()) {
            case READER:
                mShouldFinish = true;
                intent = CMSActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case NOTIFICATIONS:
                mShouldFinish = true;
                intent = CMSActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case POSTS:
                mShouldFinish = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_POSTS);
                intent = CMSActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case MEDIA:
                mShouldFinish = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_MEDIA_LIBRARY);
                intent = CMSActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case PAGES:
                mShouldFinish = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_PAGES);
                intent = CMSActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case COMMENTS:
                mShouldFinish = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_COMMENTS);
                intent = CMSActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case THEMES:
                mShouldFinish = true;
                intent = CMSActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case STATS:
                mShouldFinish = true;
                intent = CMSActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case VIEW_SITE:
                mShouldFinish = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_VIEW_SITE);
                intent = CMSActivityUtils.getIntentForActivityId(this, activityId);
                break;
            /*case QUICK_PHOTO:
                mShouldFinish = false;
                intent = new Intent(WPDrawerActivity.this, EditPostActivity.class);
                intent.putExtra("quick-media", DeviceUtils.getInstance().hasCamera(getApplicationContext())
                        ? Constants.QUICK_POST_PHOTO_CAMERA
                        : Constants.QUICK_POST_PHOTO_LIBRARY);
                break;*/
            default :
                mShouldFinish = false;
                intent = null;
                break;
        }
    }

    @Override
    @SuppressLint("NewApi")
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        boolean menuDrawerDisabled = false;

        if(getIntent() != null)
        {
            menuDrawerDisabled = getIntent().getBooleanExtra(StatsActivity.ARG_NO_MENU_DRAWER, false);
        }
        if (isStaticMenuDrawer() && !menuDrawerDisabled) {
            setContentView(R.layout.activity_drawer_static);
           /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.color_primary_dark));
            }*/
        }
        else {
            setContentView(R.layout.activity_drawer);
        }

        setSupportActionBar(getToolbar());
    }

    protected Toolbar getToolbar() {
        if (mToolbar == null) {
            mToolbar = (Toolbar) findViewById(R.id.toolbar);
        }

        return mToolbar;
    }

    @Override
    public void onStart() {
        super.onStart();
        //EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        //EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isAnimatingRefreshButton) {
            isAnimatingRefreshButton = false;
        }
        if (mShouldFinish) {
            overridePendingTransition(0, 0);
            finish();
        }
        if (mScrollPositionManager != null) {
            mScrollPositionManager.saveToPreferences(this, SCROLL_POSITION_ID);
        }
    }

   /* @Override
    protected void onResume() {
        super.onResume();
        refreshMenuDrawer();
        if (mDrawerToggle != null) {
            // Sync the toggle state after onRestoreInstanceState has occurred.
            mDrawerToggle.syncState();
        }
        if (mScrollPositionManager != null) {
            mScrollPositionManager.restoreFromPreferences(this, SCROLL_POSITION_ID);
        }
    }*/

    /**
     * Setup the global state tracking which blog is currently active if the user is signed in.
     */
    public void setupCurrentBlog() {
        if (askToSignInIfNot()) {
            //CMS.getCurrentBlog();
        }
    }

    private boolean askToSignInIfNot() {
        if (!CMS.isSignedIn(CMSDrawerActivity.this)) {
            AppLog.d(AppLog.T.NUX, "No accounts configured.  Sending user to set up an account");
            mShouldFinish = false;
            Intent intent = new Intent(this, SignInActivity.class);
            intent.putExtra("request", SignInActivity.SIGN_IN_REQUEST);
            startActivityForResult(intent, ADD_ACCOUNT_REQUEST);
            return false;
        }
        return true;
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            *//*case ADD_ACCOUNT_REQUEST:
                if (resultCode == RESULT_OK) {
                    // new blog has been added, so rebuild cache of blogs and setup current blog
                    getBlogNames();
                    setupCurrentBlog();
                    if (mDrawerListView != null) {
                        initBlogSpinner();
                    }
                    WordPress.registerForCloudMessaging(this);
                    // If logged in without blog, redirect to the Reader view
                    showCorrectActivityForAccountIfRequired();
                } else {
                    finish();
                }
                break;*//*
            *//*case SETTINGS_REQUEST:
                // user returned from settings - skip if user signed out
                if (mDrawerListView != null && resultCode != SettingsActivity.RESULT_SIGNED_OUT) {
                    // If we need to add or remove the blog spinner, init the drawer again
                    initBlogSpinner();

                    String[] blogNames = getBlogNames();
                    if (blogNames.length >= 1) {
                        setupCurrentBlog();
                    }
                    if (data != null && data.getBooleanExtra(SettingsActivity.CURRENT_BLOG_CHANGED, true)) {
                        blogChanged();
                    }
                    //WordPress.registerForCloudMessaging(this);
                }*//*

                //break;
            case AUTHENTICATE_REQUEST:
                if (resultCode == RESULT_CANCELED) {
                    Intent i = new Intent(this, SignInActivity.class);
                    startActivityForResult(i, ADD_ACCOUNT_REQUEST);
                } else {
                    //WordPress.registerForCloudMessaging(this);
                }
                break;
        }
    }
*/
    void refreshMenuDrawer() {

    }

    public boolean isStaticMenuDrawer() {
        return DisplayUtils.isLandscape(this)
                && DisplayUtils.isXLarge(this);
    }
}


