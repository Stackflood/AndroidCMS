package com.example.manish.androidcms.datasets;

import android.database.sqlite.SQLiteDatabase;

/**
 * stores comments on reader posts
 */
public class ReaderCommentTable {

    private static final String COLUMN_NAMES =
            " blog_id,"
                    + " post_id,"
                    + " comment_id,"
                    + " parent_id,"
                    + " author_name,"
                    + " author_avatar,"
                    + " author_url,"
                    + " author_id,"
                    + " author_blog_id,"
                    + " published,"
                    + " timestamp,"
                    + " status,"
                    + " text,"
                    + " num_likes,"
                    + " is_liked,"
                    + " page_number";


    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_comments ("
                + " blog_id             INTEGER DEFAULT 0,"
                + " post_id             INTEGER DEFAULT 0,"
                + "	comment_id		    INTEGER DEFAULT 0,"
                + " parent_id           INTEGER DEFAULT 0,"
                + "	author_name	        TEXT,"
                + " author_avatar       TEXT,"
                + "	author_url	        TEXT,"
                + " author_id           INTEGER DEFAULT 0,"
                + " author_blog_id      INTEGER DEFAULT 0,"
                + " published           TEXT,"
                + " timestamp           INTEGER DEFAULT 0,"
                + " status              TEXT,"
                + " text                TEXT,"
                + " num_likes           INTEGER DEFAULT 0,"
                + " is_liked            INTEGER DEFAULT 0,"
                + " page_number         INTEGER DEFAULT 0,"
                + " PRIMARY KEY (blog_id, post_id, comment_id))");
        db.execSQL("CREATE INDEX idx_page_number ON tbl_comments(page_number)");
    }

    protected static int purge(SQLiteDatabase db) {
        // purge comments attached to posts that no longer exist
        int numDeleted = db.delete("tbl_comments",
                "post_id NOT IN (SELECT DISTINCT post_id FROM tbl_posts)", null);

        // purge all but the first page of comments
        numDeleted += db.delete("tbl_comments", "page_number != 1", null);

        return numDeleted;
    }


    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_comments");
    }

    protected static void reset(SQLiteDatabase db) {
        dropTables(db);
        createTables(db);
    }
}
