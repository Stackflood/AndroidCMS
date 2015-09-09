package com.example.manish.androidcms.ui.notifications;


import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.GCMIntentService;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.Note;
import com.example.manish.androidcms.ui.CMSDrawerActivity;
import com.example.manish.androidcms.ui.notifications.adapter.NotesAdapter;
import com.example.manish.androidcms.ui.notifications.utils.NotificationsUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import Rest.RestRequest;

/**
 * Created by Manish on 9/8/2015.
 */
public class NotificationsActivity extends CMSDrawerActivity {

    public static final String NOTIFICATION_ACTION = "com.example.manish.androidcms.NOTIFICATION";
    private static final String TAG_NOTES_LIST = "notesList";
    //private NotificationsListFragment mNotesListFragment;
    private NotificationsListFragment mNotesList;
    private BroadcastReceiver mBroadcastReceiver;
    private NotesAdapter mNotesAdapter;
    private boolean mFirstLoadComplete = false;
    private boolean mLoadingMore = false;
    private static final int UNSPECIFIED_NOTE_ID = -1;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //createMenuDrawer(R.layout.notifications_activity);
        createMenuDrawer(R.layout.notifications_old);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(getResources().getString(R.string.notifications));
        }

        FragmentManager fm = getFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        mNotesList = (NotificationsListFragment)fm.findFragmentById(R.id.fragment_notes_list);
        mNotesList.setNoteProvider(new NoteProvider());


        mNotesList.setOnNoteClickListener(new NoteClickListener());
        ///restoreSavedInstance(savedInstanceState);
        GCMIntentService.mActiveNotificationsMap.clear();

        if (mBroadcastReceiver == null) {
            createBroadcastReceiver();
        }
        //new with simperium
        /*if (savedInstanceState == null) {
            mNotesListFragment = new NotificationsListFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.notifications_container, mNotesListFragment,
                            TAG_NOTES_LIST).commit();

            launchWithNoteId();
        }*/
    }

    public void refreshNotes()
    {
        if(!NetworkUtils.isNetworkAvailable(this))
        {
            mNotesList.animateRefresh(false);
            return;
        }
        mFirstLoadComplete = false;

        NotesResponseHandler notesHandler = new NotesResponseHandler()
        {
            @Override
            public void onNotes(final List<Note> notes)
            {
                mFirstLoadComplete = true;
                mNotesList.setAllNotesLoaded(false);

                new Thread()
                {
                    @Override
                public void run()
                    {
                        CMS.cmsDB.saveNotes(notes, true);
                        NotificationsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshNotificationsListFragment(notes);
                                mNotesList.animateRefresh(false);
                            }
                        });
                    }
                }.start();
            }
            @Override
            public void onErrorResponse(VolleyError error){
                //We need to show an error message? and remove the loading indicator from the list?
                mFirstLoadComplete = true;
                mNotesList.getNotesAdapter().addAll(new ArrayList<Note>(), true);
               // ToastUtils.showToastOrAuthAlert(NotificationsActivity.this, error, getString(R.string.error_refresh_notifications));
                mNotesList.animateRefresh(false);
            }
        };

        NotificationsUtils.refreshNotifications(notesHandler, notesHandler);
    }

    abstract class NotesResponseHandler implements RestRequest.Listener,
            RestRequest.ErrorListener
    {
        NotesResponseHandler(){
            mLoadingMore = true;
        }
        abstract void onNotes(List<Note> notes);

        @Override
        public void onResponse(JSONObject response)
        {
            mLoadingMore = false;
            if( response == null ) {
                //Not sure this could ever happen, but make sure we're catching all response types
                AppLog.w(AppLog.T.NOTIFS, "Success, but did not receive any notes");
                onNotes(new ArrayList<Note>(0));
                return;
            }
            try {
                List<Note> notes = NotificationsUtils.parseNotes(response);
                onNotes(notes);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.NOTIFS, "Success, but can't parse the response", e);
                showError(getString(R.string.error_parsing_response));
            }
        }

        @Override
        public void onErrorResponse(VolleyError error){
            mLoadingMore = false;
            showError(getString(R.string.error_parsing_response));
            AppLog.d(AppLog.T.NOTIFS, String.format("Error retrieving notes: %s", error));
        }

        public void showError(final String errorMessage){
            Toast.makeText(NotificationsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mBroadcastReceiver, new IntentFilter(NOTIFICATION_ACTION));
    }

    private void createBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadNotes(true, UNSPECIFIED_NOTE_ID, null);
            }
        };
    }

    private interface LoadNotesCallback {
        void notesLoaded();
    }

    private void loadNotes(final boolean launchWithNoteId, final int noteId, final LoadNotesCallback callback) {
        new Thread() {
            @Override
            public void run() {
                final List<Note> notes = CMS.cmsDB.getLatestNotes();
                NotificationsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) {
                            return;
                        }
                        refreshNotificationsListFragment(notes);
                        if (launchWithNoteId) {
                            launchWithNoteId(noteId);
                        } else {
                            if (noteId != UNSPECIFIED_NOTE_ID) {
                                Note note = CMS.cmsDB.getNoteById(noteId);
                                if (note != null) {
                                    mNotesList.setNoteSelected(note, true);
                                }
                            }
                        }

                        if (callback != null) {
                            callback.notesLoaded();
                        }
                    }
                });
            }
        }.start();
    }


    /**
     * Detect if Intent has a noteId extra and display that specific note detail fragment
     */
    private void launchWithNoteId(int noteId) {
    }

    private void refreshNotificationsListFragment(List<Note> notes) {
        AppLog.d(AppLog.T.NOTIFS, "refreshing note list fragment");
        mNotesList.getNotesAdapter().addAll(notes, true);
        // mark last seen timestamp
        if (!notes.isEmpty()) {
            updateLastSeen(notes.get(0).getTimestamp());
        }
    }

    private void updateLastSeen(String timestamp) {
        CMS.getRestClientUtils().markNotificationsSeen(
                timestamp, new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.d(AppLog.T.NOTIFS, String.format("Set last seen time %s", response));
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.d(AppLog.T.NOTIFS, String.format("Could not set last seen time %s", error));
                    }
                }
        );
    }
    private class NoteProvider implements NotificationsListFragment.NoteProvider {
        @Override
        public boolean canRequestMore() {
            return mFirstLoadComplete && !mLoadingMore;
        }

        @Override
        public void onRequestMoreNotifications(){
            if (canRequestMore()) {
                NotesAdapter adapter = mNotesList.getNotesAdapter();
                if (adapter.getCount() > 0) {
                    Note lastNote = adapter.getItem(adapter.getCount()-1);
                    //requestNotesBefore(lastNote);
                }
            }
        }
    }

    private class NoteClickListener
            implements NotificationsListFragment.OnNoteClickListener {
        @Override
        public void onClickNote(Note note){
            if (note == null)
                return;
            // open the latest version of this note just in case it has changed - this can
            // happen if the note was tapped from the list fragment after it was updated
            // by another fragment (such as NotificationCommentLikeFragment)
            Note updatedNote = CMS.cmsDB.getNoteById(StringUtils.stringToInt(note.getId()));
            //openNote(updatedNote != null ? updatedNote : note, false);
        }
    }

    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener =
            new FragmentManager.OnBackStackChangedListener() {
                public void onBackStackChanged() {
                    int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
                    // This is ugly, but onBackStackChanged is not called just after a fragment commit.
                    // In a 2 commits in a row case, onBackStackChanged is called twice but after the
                    // 2 commits. That's why mSelectedPostId can't be affected correctly after the first commit.
                    /*switch (backStackEntryCount) {
                        case 2:
                            mSelectedReaderPost = mTmpSelectedReaderPost;
                            mSelectedComment = mTmpSelectedComment;
                            mTmpSelectedReaderPost = null;
                            mTmpSelectedComment = null;
                            break;
                        case 1:
                            if (mDualPane) {
                                mSelectedReaderPost = mTmpSelectedReaderPost;
                                mSelectedComment = mTmpSelectedComment;
                            } else {
                                mSelectedReaderPost = null;
                                mSelectedComment = null;
                            }
                            break;
                        case 0:
                            mMenuDrawer.setDrawerIndicatorEnabled(true);
                            mSelectedReaderPost = null;
                            mSelectedComment = null;
                            break;
                    }*/
                }
            };


    /*private void launchWithNoteId() {
        if(isFinishing() || mNotesListFragment == null)
            return;

        Intent intent = getIntent();
        if(intent.hasExtra(NotificationsListFragment.NOTE_ID_EXTRA))
        {
            boolean shouldShowKeyboard = intent.getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, false);
            mNotesListFragment.openNote(intent.getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA),
                    this,
                    shouldShowKeyboard);
        }

        GCMIntentService.clearNotificationsMap();
    }*/
}
