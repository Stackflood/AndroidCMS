package com.example.manish.androidcms.ui.posts;

import android.os.Bundle;
import android.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.Post;
import com.example.manish.androidcms.models.PostLocation;
import com.example.manish.androidcms.models.PostStatus;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Manish on 5/14/2015.
 */
public class EditPostSettingsFragment extends Fragment

        implements View.OnClickListener, TextView.OnEditorActionListener
{

    private int mYear, mMonth, mDay, mHour, mMinute;

    private ArrayList<String> mCategories;
    private PostLocation mPostLocation;

    private EditText mPasswordEditText, mTagsEditText, mExcerptEditText;

    private TextView mPubDateText;
    private EditPostActivity mActivity;

    private Spinner mStatusSpinner, mPostFormatSpinner;

    private long mCustomPubDate = 0;

    private String[] mPostFormats;
    private String[] mPostFormatTitles;

    private boolean mIsCustomPubDate;

    /**
     * Updates post object with content of this fragment
     */
    public void updatePostSettings() {
        Post post = mActivity.getPost();
        if (post == null)
            return;

        String password = (mPasswordEditText.getText() != null) ? mPasswordEditText.getText().toString() : "";
        String pubDate = (mPubDateText.getText() != null) ? mPubDateText.getText().toString() : "";
        String excerpt = (mExcerptEditText.getText() != null) ? mExcerptEditText.getText().toString() : "";

        long pubDateTimestamp = 0;
        if (mIsCustomPubDate && pubDate.equals(getResources().getText(R.string.immediately)) && !post.isLocalDraft()) {
            Date d = new Date();
            pubDateTimestamp = d.getTime();
        } else if (!pubDate.equals(getResources().getText(R.string.immediately))) {
            if (mIsCustomPubDate)
                  pubDateTimestamp = mCustomPubDate;
            else if (post.getDate_created_gmt() > 0)
                pubDateTimestamp = post.getDate_created_gmt();
        } else if (pubDate.equals(getResources().getText(R.string.immediately)) && post.isLocalDraft()) {
            post.setDate_created_gmt(0);
            post.setDateCreated(0);
        }

        String tags = "", postFormat = "";
        if (!post.isPage()) {
            tags = (mTagsEditText.getText() != null) ? mTagsEditText.getText().toString() : "";

            // post format
            if (mPostFormats != null && mPostFormatSpinner.getSelectedItemPosition() < mPostFormats.length) {
                postFormat = mPostFormats[mPostFormatSpinner.getSelectedItemPosition()];
            }
        }

        String status = getPostStatusForSpinnerPosition(mStatusSpinner.getSelectedItemPosition());

        // We want to flag this post as having changed statuses from draft to published so that we
        // properly track stats we care about for when users first publish posts.
        if (post.isUploaded() && post.getPostStatus().equals(PostStatus.toString(PostStatus.DRAFT))
                && status.equals(PostStatus.toString(PostStatus.PUBLISHED))) {
            post.setChangedFromLocalDraftToPublished(true);
        }

        if (post.supportsLocation()) {
            post.setLocation(mPostLocation);
        }

        post.setPostExcerpt(excerpt);
        post.setDate_created_gmt(pubDateTimestamp);
        post.setJSONCategories(new JSONArray(mCategories));
        post.setKeywords(tags);
        post.setPostStatus(status);
        post.setPassword(password);
        post.setPostFormat(postFormat);
    }

    private String getPostStatusForSpinnerPosition(int position) {
        switch (position) {
            case 0:
                return PostStatus.toString(PostStatus.PUBLISHED);
            case 1:
                return PostStatus.toString(PostStatus.DRAFT);
            case 2:
                return PostStatus.toString(PostStatus.PENDING);
            case 3:
                return PostStatus.toString(PostStatus.PRIVATE);
            default:
                return PostStatus.toString(PostStatus.UNKNOWN);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);
        mCategories = new ArrayList<String>();

        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.fragment_edit_post_settings, container, false);

        if (rootView == null) {
            return null;
        }

        mActivity = (EditPostActivity) getActivity();

        mStatusSpinner = (Spinner) rootView.findViewById(R.id.status);
        mStatusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View view,
                                       int position, long id) {
                updatePostSettingsAndSaveButton();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mPostFormatTitles = getResources().getStringArray(R.array.post_formats_array);
        mPostFormatSpinner = (Spinner)rootView.findViewById(R.id.postFormat);
        ArrayAdapter<String> pfAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item,
                mPostFormatTitles);
        mPostFormatSpinner.setAdapter(pfAdapter);

        mPostFormatSpinner.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        return false;
                    }
                }
        );

        Post post = mActivity.getPost();
        if(post != null)
        {
            String[] items = new String[]{getResources().getString(R.string.publish_post), getResources().getString(R.string.draft),
                    getResources().getString(R.string.pending_review),
                    getResources().getString(R.string.post_private)};

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mStatusSpinner.setAdapter(adapter);
            mStatusSpinner.setOnTouchListener(
                    new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            return false;
                        }
                    }
            );
        }

        switch (post.getStatusEnum()) {
            case PUBLISHED:
            case SCHEDULED:
            case UNKNOWN:
                mStatusSpinner.setSelection(0, true);
                break;
            case DRAFT:
                mStatusSpinner.setSelection(1, true);
                break;
            case PENDING:
                mStatusSpinner.setSelection(2, true);
                break;
            case PRIVATE:
                mStatusSpinner.setSelection(3, true);
                break;
        }



        return rootView;
    }

    // Saves settings to post object and updates save button text in the ActionBar

    private void updatePostSettingsAndSaveButton() {
        /*if (mActivity != null) {
            updatePostSettings();
            mActivity.invalidateOptionsMenu();
        }*/
    }

    @Override
    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        boolean handled = false;
        int id = view.getId();
        /*if (id == R.id.searchLocationText && actionId == EditorInfo.IME_ACTION_SEARCH) {
            searchLocation();
            handled = true;
        }*/
        return handled;
    }

    @Override
    public void onClick(View v) {

    }
}
