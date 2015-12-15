package com.example.manish.androidcms.models;

/**
 * Created by Manish on 4/13/2015.
 */

/**
 * A Model for parsing the result of wpcom.getFeatures() to retrieve
 * features for a hosted WordPress.com blog.
 */
public class FeatureSet {

    private boolean mIsVideopressEnabled = false;

    public boolean isVideopressEnabled() {
        return mIsVideopressEnabled;
    }
}
