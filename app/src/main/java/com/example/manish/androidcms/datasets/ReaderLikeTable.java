package com.example.manish.androidcms.datasets;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.example.manish.androidcms.models.ReaderPost;
import com.example.manish.androidcms.ui.prefs.AppPrefs;

/**
 * stores likes for Reader posts and comments
 */
public class ReaderLikeTable {

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_post_likes ("
                + " post_id        INTEGER DEFAULT 0,"
                + " blog_id        INTEGER DEFAULT 0,"
                + " user_id        INTEGER DEFAULT 0,"
                + " PRIMARY KEY (blog_id, post_id, user_id))");

        db.execSQL("CREATE TABLE tbl_comment_likes ("
                + " comment_id     INTEGER DEFAULT 0,"
                + " blog_id        INTEGER DEFAULT 0,"
                + " user_id        INTEGER DEFAULT 0,"
                + " PRIMARY KEY (blog_id, comment_id, user_id))");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_post_likes");
        db.execSQL("DROP TABLE IF EXISTS tbl_comment_likes");
    }

    /*
     * purge likes attached to posts/comments that no longer exist
     */
    protected static int purge(SQLiteDatabase db) {
        int numDeleted = db.delete("tbl_post_likes",
                "post_id NOT IN (SELECT DISTINCT post_id FROM tbl_posts)", null);
        numDeleted += db.delete("tbl_comment_likes",
                "comment_id NOT IN (SELECT DISTINCT comment_id FROM tbl_comments)", null);
        return numDeleted;
    }

    public static void setCurrentUserLikesPost(ReaderPost post, boolean isLiked) {
        if (post == null) {
            return;
        }
        long currentUserId = AppPrefs.getCurrentUserId();
        if (isLiked) {
            ContentValues values = new ContentValues();
            values.put("blog_id", post.blogId);
            values.put("post_id", post.postId);
            values.put("user_id", currentUserId);
            ReaderDatabase.getWritableDb().insert("tbl_post_likes", null, values);
        } else {
            String args[] = {Long.toString(post.blogId), Long.toString(post.postId), Long.toString(currentUserId)};
            ReaderDatabase.getWritableDb().delete("tbl_post_likes", "blog_id=? AND post_id=? AND user_id=?", args);
        }
    }
}
