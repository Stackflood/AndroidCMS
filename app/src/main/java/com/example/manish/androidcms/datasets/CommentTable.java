package com.example.manish.androidcms.datasets;

import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.util.AppLog;

/**
 * Created by Manish on 4/1/2015.
 */
public class CommentTable {

    private static final String COMMENTS_TABLE = "comments";

    public static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + COMMENTS_TABLE + " ("
                + "    blog_id             INTEGER DEFAULT 0,"
                + "    post_id             INTEGER DEFAULT 0,"
                + "    comment_id          INTEGER DEFAULT 0,"
                + "    comment             TEXT,"
                + "    published           TEXT,"
                + "    status              TEXT,"
                + "    author_name         TEXT,"
                + "    author_url          TEXT,"
                + "    author_email        TEXT,"
                + "    post_title          TEXT,"
                + "    profile_image_url   TEXT,"
                + "    PRIMARY KEY (blog_id, post_id, comment_id)"
                + " );");
    }

    private static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + COMMENTS_TABLE);
    }

    public static void reset(SQLiteDatabase db) {
        AppLog.i(AppLog.T.COMMENTS, "resetting comment table");
        dropTables(db);
        createTables(db);
    }
}
