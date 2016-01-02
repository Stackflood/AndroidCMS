package com.example.manish.androidcms.models;

import android.text.TextUtils;

import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

/**
 * Created by Manish on 12/30/2015.
 */
public class ReaderBlog {

    public long blogId;
    public long feedId;

    public boolean isPrivate;
    public boolean isJetpack;
    public boolean isFollowing;
    public int numSubscribers;

    private String name;
    private String description;
    private String url;
    private String imageUrl;
    private String feedUrl;

    public String getName() {
        return StringUtils.notNullStr(name);
    }
    public void setName(String blogName) {
        this.name = StringUtils.notNullStr(blogName).trim();
    }

    public String getDescription() {
        return StringUtils.notNullStr(description);
    }
    public void setDescription(String description) {
        this.description = StringUtils.notNullStr(description).trim();
    }

    public String getImageUrl() {
        return StringUtils.notNullStr(imageUrl);
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = StringUtils.notNullStr(imageUrl);
    }

    public String getUrl() {
        return StringUtils.notNullStr(url);
    }
    public void setUrl(String url) {
        this.url = StringUtils.notNullStr(url);
    }

    public String getFeedUrl() {
        return StringUtils.notNullStr(feedUrl);
    }
    public void setFeedUrl(String feedUrl) {
        this.feedUrl = StringUtils.notNullStr(feedUrl);
    }

    public boolean hasUrl() {
        return !TextUtils.isEmpty(url);
    }
    public boolean hasImageUrl() {
        return !TextUtils.isEmpty(imageUrl);
    }
    public boolean hasName() {
        return !TextUtils.isEmpty(name);
    }
    public boolean hasDescription() {
        return !TextUtils.isEmpty(description);
    }

    public boolean isExternal() {
        return (feedId != 0);
    }

    /*
     * returns the mshot url to use for this blog, ex:
     *   http://s.wordpress.com/mshots/v1/http%3A%2F%2Fnickbradbury.com?w=600
     * note that while mshots support a "h=" parameter, this crops rather than
     * scales the image to that height
     *   https://github.com/Automattic/mShots
     */
    public String getMshotsUrl(int width) {
        return "http://s.wordpress.com/mshots/v1/"
                + UrlUtils.urlEncode(getUrl())
                + String.format("?w=%d", width);
    }
}
