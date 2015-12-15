package com.example.manish.androidcms.ui.notifications;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;


import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.Note;
import com.example.manish.androidcms.ui.EmptyViewMessageType;
import com.example.manish.androidcms.ui.notifications.adapter.NotesAdapter;
import com.example.manish.androidcms.util.widgets.CustomSwipeRefreshLayout;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
/**
 * Created by Manish on 9/8/2015.
 */
public class NotificationsListFragment extends ListFragment

        implements NotesAdapter.DataLoadedListener{
    private static final int LOAD_MORE_WITHIN_X_ROWS = 5;

    public static final String NOTE_ID_EXTRA = "noteId";
    public static final String NOTE_INSTANT_REPLY_EXTRA = "instantReply";
    public static final String NOTE_MODERATE_ID_EXTRA = "moderateNoteId";
    public static final String NOTE_MODERATE_STATUS_EXTRA = "moderateNoteStatus";
    private static final int NOTE_DETAIL_REQUEST_CODE = 0;
    private View mProgressFooterView;
    private boolean mAllNotesLoaded;
    private NotesAdapter mNotesAdapter;

    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private boolean mSwipedToRefresh;

    private NoteProvider mNoteProvider;

    NotesAdapter getNotesAdapter() {
        if (mNotesAdapter == null) {
            mNotesAdapter = new NotesAdapter(getActivity(), this);

        }
        return mNotesAdapter;
    }

    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.empty_listview, container, false);
    }

    public void onActivityCreated(Bundle bundle)
    {
        super.onActivityCreated(bundle);
        mProgressFooterView = View.inflate(getActivity(),
                R.layout.list_footer_progress, null);

        mProgressFooterView.setVisibility(View.GONE);

        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnScrollListener(new ListScrollListener());
        listView.setDivider(getResources().getDrawable(R.drawable.list_divider));

        listView.setDividerHeight(1);
        listView.addFooterView(mProgressFooterView, null, false);
        setListAdapter(getNotesAdapter());

        // Set empty text if no notifications
        TextView textview = (TextView) listView.getEmptyView();
        if (textview != null) {
            textview.setText(getText(R.string.notifications_empty_list));
        }

       //initSwipeToRefreshHelper();
    }

    @Override
    public void onDestroy() {
        // Close Simperium cursor
        if (mNotesAdapter != null) {
            mNotesAdapter.closeCursor();
        }

        super.onDestroy();
    }

    public void animateRefresh(boolean refresh) {
        mSwipeToRefreshHelper.setRefreshing(refresh);
    }

    void setAllNotesLoaded(boolean allNotesLoaded) {
        mAllNotesLoaded = allNotesLoaded;
    }


    private void initSwipeToRefreshHelper() {
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(
                getActivity(),
                (org.wordpress.android.util.widgets.CustomSwipeRefreshLayout) getView().findViewById(R.id.ptr_layout),
                new SwipeToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if(!isAdded())
                        {
                            return;
                        }
                        if(!NetworkUtils.checkConnection(getActivity()))
                        {
                            mSwipeToRefreshHelper.setRefreshing(false);
                            //updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
                            return;
                        }
                        mSwipedToRefresh = true;
                        ((NotificationsActivity) getActivity()).refreshNotes();
                    }
                });


    }

    private class ListScrollListener implements AbsListView.OnScrollListener {
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            if (visibleItemCount == 0 || totalItemCount == 0)
                return;

            // skip if all notes are loaded or notes are currently being added to the adapter
            if (mAllNotesLoaded || getNotesAdapter().isAddingNotes())
                return;

            // if we're within 5 from the last item we should ask for more items
            if (firstVisibleItem + visibleItemCount >= totalItemCount - LOAD_MORE_WITHIN_X_ROWS) {
                requestMoreNotifications();
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }
    }
    private boolean hasActivity() {
        return (getActivity() != null && !isRemoving());
    }

    /*
    * show/hide the "Loading" footer
    */
    private void showProgressFooter() {
        if (mProgressFooterView != null)
            mProgressFooterView.setVisibility(View.VISIBLE);
    }

    private void requestMoreNotifications() {
        if (getView() == null) {
            AppLog.w(AppLog.T.NOTIFS, "requestMoreNotifications called before view exists");
            return;
        }

        if (!hasActivity()) {
            AppLog.w(AppLog.T.NOTIFS, "requestMoreNotifications called without activity");
            return;
        }

        if (mNoteProvider != null && mNoteProvider.canRequestMore()) {
            showProgressFooter();
            mNoteProvider.onRequestMoreNotifications();
        }
    }

    private void hideProgressFooter() {
        if (mProgressFooterView != null)
            mProgressFooterView.setVisibility(View.GONE);
    }
    /*
     * called by NotesAdapter after loading notes
     */
    @Override
    public void onDataLoaded(boolean isEmpty) {
        hideProgressFooter();
    }
    /**
     * For providing more notes data when getting to the end of the list
     */
    public interface NoteProvider {
        public boolean canRequestMore();
        public void onRequestMoreNotifications();
    }
    /**
     * Open a note fragment based on the type of note
     */
    public void openNote(final String noteId, Activity activity, boolean shouldShowKeyboard) {
        if (noteId == null || activity == null) {
            return;
        }

        Intent detailIntent = new Intent(activity, NotificationsDetailActivity.class);
        detailIntent.putExtra(NOTE_ID_EXTRA, noteId);
        detailIntent.putExtra(NOTE_INSTANT_REPLY_EXTRA, shouldShowKeyboard);

        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.reader_activity_slide_in,
                R.anim.reader_activity_scale_out);
        ActivityCompat.startActivityForResult(activity, detailIntent, NOTE_DETAIL_REQUEST_CODE, options.toBundle());

        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS);
    }

    public void setNoteProvider(NoteProvider provider) {
        mNoteProvider = provider;
    }

    private OnNoteClickListener mNoteClickListener;

    public void setOnNoteClickListener(OnNoteClickListener listener) {
        mNoteClickListener = listener;
    }

    public void setNoteSelected(Note note, boolean scrollToNote)
    {

    }

    /**
     * For responding to tapping of notes
     */
    public interface OnNoteClickListener {
        public void onClickNote(Note note);
    }

}
