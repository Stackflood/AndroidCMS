package com.example.manish.androidcms.ui.posts;

import android.app.Fragment;

import com.example.manish.androidcms.models.Post;

/**
 * Created by Manish on 4/1/2015.
 */
public class ViewPostFragment extends Fragment {
    /** Called when the activity is first created. */
    public interface OnDetailPostActionListener {
        public void onDetailPostAction(int action, Post post);
    }
}
