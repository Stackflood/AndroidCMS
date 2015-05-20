package com.example.manish.androidcms.datasets;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.models.Comment;
import com.example.manish.androidcms.models.CommentList;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;

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

    /**
     * nbradbury - saves comments for passed blog to local db, overwriting existing ones if necessary
     * @param localBlogId - unique id in account table for this blog
     * @param comments - list of comments to save
     * @return true if saved, false on failure
     */
    public static boolean saveComments(int localBlogId, final CommentList comments) {
        if (comments == null || comments.size() == 0)
            return false;

        final String sql = " INSERT OR REPLACE INTO " + COMMENTS_TABLE + "("
                + " blog_id,"          // 1
                + " post_id,"          // 2
                + " comment_id,"       // 3
                + " comment,"          // 4
                + " published,"        // 5
                + " status,"           // 6
                + " author_name,"      // 7
                + " author_url,"       // 8
                + " author_email,"     // 9
                + " post_title,"       // 10
                + " profile_image_url" // 11
                + " ) VALUES (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11)";

        SQLiteDatabase db = getWritableDb();
        SQLiteStatement stmt = db.compileStatement(sql);
        db.beginTransaction();
        try {
            try {
                for (Comment comment: comments) {
                    stmt.bindLong  ( 1, localBlogId);
                    stmt.bindLong  ( 2, comment.postID);
                    stmt.bindLong  ( 3, comment.commentID);
                    stmt.bindString( 4, comment.getCommentText());
                    stmt.bindString( 5, comment.getPublished());
                    stmt.bindString( 6, comment.getStatus());
                    stmt.bindString( 7, comment.getAuthorName());
                    stmt.bindString( 8, comment.getAuthorUrl());
                    stmt.bindString( 9, comment.getAuthorEmail());
                    stmt.bindString(10, comment.getPostTitle());
                    stmt.bindString(11, comment.getProfileImageUrl());
                    stmt.execute();
                }

                db.setTransactionSuccessful();
                return true;
            } catch (SQLiteException e) {
                AppLog.e(AppLog.T.COMMENTS, e);
                return false;
            }
        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }


    private static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + COMMENTS_TABLE);
    }

    public static void reset(SQLiteDatabase db) {
        AppLog.i(AppLog.T.COMMENTS, "resetting comment table");
        dropTables(db);
        createTables(db);
    }

    private static SQLiteDatabase getWritableDb() {
        return CMS.cmsDB.getDatabase();
    }

    /**
     * nbradbury 11/12/13 - delete a single comment
     * @param localBlogId - unique id in account table for this blog
     * @param commentId - commentId of the actual comment
     * @return true if comment deleted, false otherwise
     */
    public static boolean deleteComment(int localBlogId, long commentId) {
        String[] args = {Integer.toString(localBlogId),
                Long.toString(commentId)};
        int count = getWritableDb().delete(COMMENTS_TABLE, "blog_id=? AND comment_id=?", args);
        return (count > 0);
    }
}
