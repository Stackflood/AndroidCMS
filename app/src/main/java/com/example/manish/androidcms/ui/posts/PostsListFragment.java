package com.example.manish.androidcms.ui.posts;

import android.app.ListFragment;
import android.os.Bundle;

import com.example.manish.androidcms.models.Post;

/**
 * Created by Manish on 4/1/2015.
 */
public class PostsListFragment extends ListFragment {

    private boolean mIsPage, mShouldSelectFirstPost, mIsFetchingPosts;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (isAdded()) {

            Bundle extras = getActivity().getIntent().getExtras();
            if(extras != null)
            {
                mIsPage = extras.getBoolean(PostsActivity.EXTRA_VIEW_PAGES);
            }
        }
    }

    /*onActivityCreated(Bundle savedInstanceState)
    Called when the fragment's activity has been created and this fragment's view hierarchy instantiated*/

    public void setShouldSelectFirstPost(boolean shouldSelect) {
        mShouldSelectFirstPost = shouldSelect;
    }

    public interface OnPostSelectedListener {
        public void onPostSelected(Post post);
    }

    public interface OnPostActionListener {
        public void onPostAction(int action, Post post);
    }

    public interface OnSinglePostLoadedListener {
        public void onSinglePostLoaded();
    }
}
