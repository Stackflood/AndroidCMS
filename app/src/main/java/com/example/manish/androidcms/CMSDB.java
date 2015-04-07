package com.example.manish.androidcms;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.example.manish.androidcms.datasets.CommentTable;
import com.example.manish.androidcms.datasets.SuggestionTable;

import org.wordpress.android.util.SqlUtils;

import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;


//import  commons

/**
 * Created by Manish on 4/1/2015.
 */
public class CMSDB {

    // add hidden flag to blog settings (accounts)
    private static final String ADD_ACCOUNTS_HIDDEN_FLAG = "alter table accounts add isHidden boolean default 0;";
    private SQLiteDatabase db;
    private Context context;
    private static final String DATABASE_NAME = "CMS";
    private static final String CREATE_TABLE_SETTINGS = "create table if not exists accounts (id integer primary key autoincrement, "
            + "url text, blogName text, username text, password text, imagePlacement text, centerThumbnail boolean, fullSizeImage boolean, maxImageWidth text, maxImageWidthId integer);";

    public static final String SETTINGS_TABLE = "accounts";

    private static final String CREATE_TABLE_POSTS = "create table if not exists posts (id integer primary key autoincrement, blogID text, "
            + "postid text, title text default '', dateCreated date, date_created_gmt date, categories text default '', custom_fields text default '', "
            + "description text default '', link text default '', mt_allow_comments boolean, mt_allow_pings boolean, "
            + "mt_excerpt text default '', mt_keywords text default '', mt_text_more text default '', permaLink text default '', post_status text default '', userid integer default 0, "
            + "wp_author_display_name text default '', wp_author_id text default '', wp_password text default '', wp_post_format text default '', wp_slug text default '', mediaPaths text default '', "
            + "latitude real, longitude real, localDraft boolean default 0, uploaded boolean default 0, isPage boolean default 0, wp_page_parent_id text, wp_page_parent_title text);";

    private static final String POSTS_TABLE = "posts";

    // categories
    private static final String CREATE_TABLE_CATEGORIES = "create table if not exists cats (id integer primary key autoincrement, "
            + "blog_id text, wp_id integer, category_name text not null);";
    private static final String CATEGORIES_TABLE = "cats";

    // for capturing blogID
    private static final String ADD_BLOGID = "alter table accounts add blogId integer;";
    private static final String UPDATE_BLOGID = "update accounts set blogId = 1;";
    private static final String ADD_LOCATION_FLAG = "alter table accounts add location boolean default false;";
    private static final String ADD_DOTCOM_USERNAME = "alter table accounts add dotcom_username text;";
    private static final String ADD_DOTCOM_PASSWORD = "alter table accounts add dotcom_password text;";
    private static final String ADD_API_KEY = "alter table accounts add api_key text;";
    private static final String ADD_API_BLOGID = "alter table accounts add api_blogid text;";
    // add wordpress.com flag and version column
    private static final String ADD_DOTCOM_FLAG = "alter table accounts add dotcomFlag boolean default false;";
    private static final String ADD_CMS_VERSION = "alter table accounts add cmsVersion text;";

    private static final int DATABASE_VERSION = 1;

    /**
     * Set the ID of the most recently active blog. This value will persist between application
     * launches.
     *
     * @param id ID of the most recently active blog.
     */
    public void updateLastBlogId(int id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("last_blog_id", id);
        editor.commit();
    }

    public static void deleteDatabase(Context ctx) {
        ctx.deleteDatabase(DATABASE_NAME);
    }

    public CMSDB(Context ctx) {
        this.context = ctx;
        db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
        // Create tables if they don't exist
        db.execSQL(CREATE_TABLE_SETTINGS);
        db.execSQL(CREATE_TABLE_POSTS);
        db.execSQL(CREATE_TABLE_CATEGORIES);

        CommentTable.createTables(db);
        SuggestionTable.createTables(db);

        // Update tables for new installs and app updates
        int currentVersion = db.getVersion();
        switch (currentVersion) {
            case 0:
                // New install
                currentVersion++;
            case 1:
                // Add columns that were added in very early releases, then move on to version 9
                db.execSQL(ADD_BLOGID);
                db.execSQL(UPDATE_BLOGID);
                db.execSQL(ADD_LOCATION_FLAG);
                db.execSQL(ADD_DOTCOM_USERNAME);
                db.execSQL(ADD_DOTCOM_PASSWORD);
                db.execSQL(ADD_API_KEY);
                db.execSQL(ADD_API_BLOGID);
                db.execSQL(ADD_DOTCOM_FLAG);
                db.execSQL(ADD_CMS_VERSION);

                /*This is meant for version - 20, just add it now*/
                db.execSQL(ADD_ACCOUNTS_HIDDEN_FLAG);
                currentVersion = 9;
        }

        db.setVersion(DATABASE_VERSION);

    }

    public List<Map<String, Object>> getAllAccounts() {
        return getAccountsBy(null, null);
    }

    public List<Map<String, Object>> getAccountsBy(String byString, String[] extraFields) {
        return getAccountsBy(byString, extraFields, 0);
    }

    public List<Integer> getAllAccountIDs() {
        Cursor c = db.rawQuery("SELECT DISTINCT id FROM " + SETTINGS_TABLE, null);
        try {
            List<Integer> ids = new ArrayList<Integer>();
            if (c.moveToFirst()) {
                do {
                    ids.add(c.getInt(0));
                } while (c.moveToNext());
            }
            return ids;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public SQLiteDatabase getDatabase() {
        return db;
    }

    public List<Map<String, Object>> getAccountsBy(String byString, String[] extraFields, int limit) {
        if (db == null) {
            return new Vector<Map<String, Object>>();
        }
        String limitStr = null;
        if (limit != 0) {
            limitStr = String.valueOf(limit);
        }
        String[] baseFields = new String[]{"id", "blogName", "username", "blogId", "url"};
        String[] allFields = baseFields;
        if (extraFields != null) {

           allFields = (String[]) ArrayUtils.addAll(baseFields, extraFields);
        }
        Cursor c = db.query(SETTINGS_TABLE, allFields, byString, null, null, null, null, limitStr);
        int numRows = c.getCount();
        c.moveToFirst();
        List<Map<String, Object>> accounts = new Vector<Map<String, Object>>();
        for (int i = 0; i < numRows; i++) {
            int id = c.getInt(0);
            String blogName = c.getString(1);
            String username = c.getString(2);
            int blogId = c.getInt(3);
            String url = c.getString(4);
            if (id > 0) {
                Map<String, Object> thisHash = new HashMap<String, Object>();
                thisHash.put("id", id);
                thisHash.put("blogName", blogName);
                thisHash.put("username", username);
                thisHash.put("blogId", blogId);
                thisHash.put("url", url);
                int extraFieldsIndex = baseFields.length;
                if (extraFields != null) {
                    for (int j = 0; j < extraFields.length; ++j) {
                        thisHash.put(extraFields[j], c.getString(extraFieldsIndex + j));
                    }
                }
                accounts.add(thisHash);
            }
            c.moveToNext();
        }
        c.close();
        //Collections.sort(accounts, BlogUtils.BlogNameComparator);
        return accounts;
    }

    public int getNumVisibleAccounts() {
        return SqlUtils.intForQuery(db, "SELECT COUNT(*) FROM " + SETTINGS_TABLE + " WHERE isHidden = 0", null);
    }
}
