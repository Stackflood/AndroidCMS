package com.example.manish.androidcms.datasets;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.manish.androidcms.models.ReaderBlog;

import org.wordpress.android.util.SqlUtils;

/**
 * tbl_blog_info contains information about blogs viewed in the reader, and blogs the
 * user is following. Note that this table is populated from two endpoints:
 *
 *      1. sites/{$siteId}
 *      2. read/following/mine?meta=site,feed
 *
 *  The first endpoint is called when the user views blog preview, the second is called
 *  to get the full list of blogs the user is following
 */
public class ReaderBlogTable {

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_blog_info ("
                + "    blog_id       INTEGER DEFAULT 0,"   // will be same as feedId for feeds
                + "    feed_id       INTEGER DEFAULT 0,"   // will be 0 for blogs
                + "	blog_url      TEXT NOT NULL COLLATE NOCASE,"
                + "    image_url     TEXT,"
                + "    feed_url      TEXT,"
                + "    name          TEXT,"
                + "    description   TEXT,"
                + "    is_private    INTEGER DEFAULT 0,"
                + "    is_jetpack    INTEGER DEFAULT 0,"
                + "    is_following  INTEGER DEFAULT 0,"
                + "    num_followers INTEGER DEFAULT 0,"
                + "    PRIMARY KEY (blog_id, feed_id)"
                + ")");

        db.execSQL("CREATE TABLE tbl_recommended_blogs ("
                + "     blog_id         INTEGER DEFAULT 0,"
                + "     follow_reco_id  INTEGER DEFAULT 0,"
                + "     score           INTEGER DEFAULT 0,"
                + "	    title           TEXT COLLATE NOCASE,"
                + "	    blog_url        TEXT COLLATE NOCASE,"
                + "	    image_url       TEXT,"
                + "	    reason          TEXT,"
                + "     PRIMARY KEY (blog_id)"
                + ")");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_blog_info");
        db.execSQL("DROP TABLE IF EXISTS tbl_recommended_blogs");
    }

    public static ReaderBlog getFeedInfo(long feedId) {
        if (feedId == 0) {
            return null;
        }
        String[] args = {Long.toString(feedId)};
        Cursor cursor = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_blog_info WHERE feed_id=?", args);
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return getBlogInfoFromCursor(cursor);
        } finally {
            SqlUtils.closeCursor(cursor);
        }
    }

    public static void setIsFollowedFeedId(long feedId, boolean isFollowed) {
        ReaderDatabase.getWritableDb().execSQL(
                "UPDATE tbl_blog_info SET is_following="
                        + SqlUtils.boolToSql(isFollowed)
                        + " WHERE feed_id=?",
                new String[]{Long.toString(feedId)});
    }

    private static ReaderBlog getBlogInfoFromCursor(Cursor c) {
        if (c == null) {
            return null;
        }

        ReaderBlog blogInfo = new ReaderBlog();
        blogInfo.blogId = c.getLong(c.getColumnIndex("blog_id"));
        blogInfo.feedId = c.getLong(c.getColumnIndex("feed_id"));
        blogInfo.setUrl(c.getString(c.getColumnIndex("blog_url")));
        blogInfo.setImageUrl(c.getString(c.getColumnIndex("image_url")));
        blogInfo.setFeedUrl(c.getString(c.getColumnIndex("feed_url")));
        blogInfo.setName(c.getString(c.getColumnIndex("name")));
        blogInfo.setDescription(c.getString(c.getColumnIndex("description")));
        blogInfo.isPrivate = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_private")));
        blogInfo.isJetpack = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_jetpack")));
        blogInfo.isFollowing = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_following")));
        blogInfo.numSubscribers = c.getInt(c.getColumnIndex("num_followers"));

        return blogInfo;
    }
}
