package com.example.manish.androidcms.models;

import android.text.TextUtils;

import com.example.manish.androidcms.ui.reader.utils.ReaderUtils;

import org.wordpress.android.util.StringUtils;

import java.io.Serializable;

/**
 * Created by Manish on 12/29/2015.
 */
public class ReaderTag implements Serializable {

    private String tagName;
    private String endpoint;
    public final ReaderTagType tagType;
    private static final String TAG_NAME_FRESHLY_PRESSED = "Freshly Pressed";
    private static final String TAG_NAME_DEFAULT = TAG_NAME_FRESHLY_PRESSED;


    /*
     * returns the tag name for use in the application log - if this is a default tag it returns
     * the full tag name, otherwise it abbreviates the tag name since exposing followed tags
     * in the log could be considered a privacy issue
     */
    public String getTagNameForLog() {
        String tagName = getTagName();
        if (tagType == ReaderTagType.DEFAULT) {
            return tagName;
        } else if (tagName.length() >= 6) {
            return tagName.substring(0, 3) + "...";
        } else if (tagName.length() >= 4) {
            return tagName.substring(0, 2) + "...";
        } else if (tagName.length() >= 2) {
            return tagName.substring(0, 1) + "...";
        } else {
            return "...";
        }
    }

    public boolean isBlogsIFollow() {
        return getTagName().equals(TAG_NAME_FOLLOWING);
    }
    public boolean isPostsILike() {
        return getTagName().equals(TAG_NAME_LIKED);
    }


    public boolean isFreshlyPressed() {
        return getTagName().equals(TAG_NAME_FRESHLY_PRESSED);
    }

    // these are the default tag names, which aren't localized in the /read/menu/ response
    public static final String TAG_NAME_FOLLOWING = "Blogs I Follow";
    private static final String TAG_NAME_LIKED = "Posts I Like";

    public static boolean isSameTag(ReaderTag tag1, ReaderTag tag2) {
        if (tag1 == null || tag2 == null) {
            return false;
        }
        return tag1.tagType == tag2.tagType
                && tag1.getSanitizedTagName().equalsIgnoreCase(tag2.getSanitizedTagName());
    }

    public String getCapitalizedTagName() {
        if (tagName == null || tagName.length() == 0) {
            return "";
        }
        // If already uppercase, assume correctly formatted
        if (Character.isUpperCase(tagName.charAt(0))) {
            return tagName;
        }
        // Accounts for iPhone, ePaper, etc.
        if (tagName.length() > 1 &&
                Character.isLowerCase(tagName.charAt(0)) &&
                Character.isUpperCase(tagName.charAt(1))) {
            return tagName;
        }
        // Capitalize anything else.
        return StringUtils.capitalize(tagName);
    }

    private String getSanitizedTagName() {
        return ReaderUtils.sanitizeWithDashes(this.tagName);
    }

    public ReaderTag(String tagName, String endpoint, ReaderTagType tagType) {
        if (TextUtils.isEmpty(tagName)) {
            this.setTagName(getTagNameFromEndpoint(endpoint));
        } else {
            this.setTagName(tagName);
        }
        this.setEndpoint(endpoint);
        this.tagType = tagType;
    }
    /*
         * extracts the tag name from a valid read/tags/[tagName]/posts endpoint
         */
    private static String getTagNameFromEndpoint(final String endpoint) {
        if (TextUtils.isEmpty(endpoint))
            return "";

        // make sure passed endpoint is valid
        if (!endpoint.endsWith("/posts"))
            return "";
        int start = endpoint.indexOf("/read/tags/");
        if (start == -1)
            return "";

        // skip "/read/tags/" then find the next "/"
        start += 11;
        int end = endpoint.indexOf("/", start);
        if (end == -1)
            return "";

        return endpoint.substring(start, end);
    }

    void setEndpoint(String endpoint) {
        this.endpoint = StringUtils.notNullStr(endpoint);
    }

    public ReaderTag(String tagName, ReaderTagType tagType) {
        this.setTagName(tagName);
        this.tagType = tagType;
    }

    public String getEndpoint() {
        return StringUtils.notNullStr(endpoint);
    }

    public String getTagName() {
        return StringUtils.notNullStr(tagName);
    }

    public static ReaderTag getDefaultTag() {
        return new ReaderTag(TAG_NAME_DEFAULT, ReaderTagType.DEFAULT);
    }
    void setTagName(String name) {
        this.tagName = StringUtils.notNullStr(name);
    }

    /*
    * is the passed tag name one of the default tags?
    */
    public static boolean isDefaultTagName(String tagName) {
        if (TextUtils.isEmpty(tagName)) {
            return false;
        }
        return (tagName.equalsIgnoreCase(TAG_NAME_FOLLOWING)
                || tagName.equalsIgnoreCase(TAG_NAME_FRESHLY_PRESSED)
                || tagName.equalsIgnoreCase(TAG_NAME_LIKED));
    }
}
