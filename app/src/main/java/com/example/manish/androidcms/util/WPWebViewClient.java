package com.example.manish.androidcms.util;

/**
 * Created by Manish on 6/3/2015.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.webkit.WebViewClient;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.models.Blog;

/**
 * WebViewClient that is capable of handling HTTP authentication requests using the HTTP
 * username and password of the blog configured for this activity.
 */
public class WPWebViewClient extends WebViewClient {

    private final Blog mBlog;
    private String mToken;

    public WPWebViewClient(Context context, Blog blog) {
        super();
        this.mBlog = blog;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mToken = settings.getString(CMS.ACCESS_TOKEN_PREFERENCE, "");
    }
}
