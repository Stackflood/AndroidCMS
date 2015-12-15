package com.example.manish.androidcms.models;

import org.json.JSONObject;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;

/**
 * Created by Manish on 4/15/2015.
 */
public class ReaderUser {

    public long userId;
    public long blogId;
    private String userName;
    private String displayName;
    private String url;
    private String profileUrl;
    private String avatarUrl;

    public static ReaderUser fromJson(JSONObject json) {
        ReaderUser user = new ReaderUser();
        if (json==null)
            return user;

        user.userId = json.optLong("ID");
        user.blogId = json.optLong("site_ID");

        user.userName = JSONUtils.getString(json, "username");
        user.url = JSONUtils.getString(json, "URL"); // <-- this isn't necessarily a wp blog
        user.profileUrl = JSONUtils.getString(json, "profile_URL");
        user.avatarUrl = JSONUtils.getString(json, "avatar_URL");

        // "me" api call (current user) has "display_name", others have "name"
        if (json.has("display_name")) {
            user.displayName = JSONUtils.getStringDecoded(json, "display_name");
        } else {
            user.displayName = JSONUtils.getStringDecoded(json, "name");
        }

        return user;
    }

    public String getUserName() {
        return StringUtils.notNullStr(userName);
    }

    public String getDisplayName() {
        return StringUtils.notNullStr(displayName);
    }
    public void setDisplayName(String displayName) {
        this.displayName = StringUtils.notNullStr(displayName);
    }

    public String getUrl() {
        return StringUtils.notNullStr(url);
    }
    public void setUrl(String url) {
        this.url = StringUtils.notNullStr(url);
    }

    public String getProfileUrl() {
        return StringUtils.notNullStr(profileUrl);
    }
    public void setProfileUrl(String profileUrl) {
        this.profileUrl = StringUtils.notNullStr(profileUrl);
    }

    public String getAvatarUrl() {
        return StringUtils.notNullStr(avatarUrl);
    }
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = StringUtils.notNullStr(avatarUrl);
    }
}
