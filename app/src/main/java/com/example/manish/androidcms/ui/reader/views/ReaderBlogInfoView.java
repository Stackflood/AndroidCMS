package com.example.manish.androidcms.ui.reader.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.ReaderBlog;

/*
 * header view showing blog name, description, and blavatar (if it exists) for a
 * blog - designed for use in ReaderPostListFragment when previewing posts in a
 * blog (blog preview) but can be reused elsewhere - call loadBlogInfo() to show
 * the info for a specific blog
 */
public class ReaderBlogInfoView extends FrameLayout {

    public interface BlogInfoListener {
        void onBlogInfoLoaded(ReaderBlog blogInfo);
        void onBlogInfoFailed();
    }

    private BlogInfoListener mBlogInfoListener;
    private ReaderBlog mBlogInfo;

    public ReaderBlogInfoView(Context context){
        super(context);

        View view = LayoutInflater.from(context).inflate(R.layout.reader_blog_info_view, this, true);
        view.setId(R.id.layout_blog_info_view);
    }
}
