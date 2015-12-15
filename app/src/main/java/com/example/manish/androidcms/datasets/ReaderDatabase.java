package com.example.manish.androidcms.datasets;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.manish.androidcms.CMS;

import org.wordpress.android.util.AppLog;

/**
 * Created by Manish on 4/15/2015.
 */
public class ReaderDatabase extends SQLiteOpenHelper {

    protected static final String DB_NAME = "wpreader.db";
    private static final int DB_VERSION = 100;

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // for now just reset the db when upgrading, future versions may want to avoid this
        // and modify table structures, etc., on upgrade while preserving data
        AppLog.i(AppLog.T.READER, "Upgrading database from version " + oldVersion + " to version " + newVersion);
        reset(db);
    }

    public static SQLiteDatabase getReadableDb() {
        return getDatabase().getReadableDatabase();
    }
    /*
     * drop & recreate all tables (essentially clears the db of all data)
     */
    private void reset(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            dropAllTables(db);
            createAllTables(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    /*
     * version history
     *   67 - added tbl_blog_info to ReaderBlogTable
     *   68 - added author_blog_id to ReaderCommentTable
     *   69 - renamed tbl_blog_urls to tbl_followed_blogs in ReaderBlogTable
     *   70 - added author_id to ReaderCommentTable and ReaderPostTable
     *   71 - added blog_id to ReaderUserTable
     *   72 - removed tbl_followed_blogs from ReaderBlogTable
     *   73 - added tbl_recommended_blogs to ReaderBlogTable
     *   74 - added primary_tag to ReaderPostTable
     *   75 - added secondary_tag to ReaderPostTable
     *   76 - added feed_id to ReaderBlogTable
     *   77 - restructured tag tables (ReaderTagTable)
     *   78 - added tag_type to ReaderPostTable.tbl_post_tags
     *   79 - added is_likes_enabled and is_sharing_enabled to tbl_posts
     *   80 - added tbl_comment_likes in ReaderLikeTable, added num_likes to tbl_comments
     *   81 - added image_url to tbl_blog_info
     *   82 - added idx_posts_timestamp to tbl_posts
     *   83 - removed tag_list from tbl_posts
     *   84 - added tbl_attachments
     *   85 - removed tbl_attachments, added attachments_json to tbl_posts
     *   90 - added default values for all INTEGER columns that were missing them (hotfix 3.1.1)
     *   92 - added default values for all INTEGER columns that were missing them (3.2)
     *   93 - tbl_posts text is now truncated to a max length (3.3)
     *   94 - added is_jetpack to tbl_posts (3.4)
     *   95 - added page_number to tbl_comments (3.4)
     *   96 - removed tbl_tag_updates, added date_updated to tbl_tags (3.4)
     *   97 - added short_url to tbl_posts
     *   98 - added feed_id to tbl_posts
     *   99 - added feed_url to tbl_blog_info
     *  100 - changed primary key on tbl_blog_info
     */
    /*
	 *  database singleton
	 */
    private static ReaderDatabase mReaderDb;
    private final static Object mDbLock = new Object();

    public static ReaderDatabase getDatabase() {
        if (mReaderDb == null) {
            synchronized(mDbLock) {
                if (mReaderDb == null) {
                    mReaderDb = new ReaderDatabase(CMS.getContext());
                    // this ensures that onOpen() is called with a writable database (open will fail if app calls getReadableDb() first)
                    mReaderDb.getWritableDatabase();
                }
            }
        }
        return mReaderDb;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        createAllTables(db);
    }
    private void createAllTables(SQLiteDatabase db) {
       /* ReaderCommentTable.createTables(db);
        ReaderLikeTable.createTables(db);
        ReaderPostTable.createTables(db);
        ReaderTagTable.createTables(db);*/
        ReaderUserTable.createTables(db);
        /*ReaderThumbnailTable.createTables(db);
        ReaderBlogTable.createTables(db);*/
    }


    private void dropAllTables(SQLiteDatabase db) {
        /*ReaderCommentTable.dropTables(db);
        ReaderLikeTable.dropTables(db);
        ReaderPostTable.dropTables(db);
        ReaderTagTable.dropTables(db);*/
        ReaderUserTable.dropTables(db);
        /*
        ReaderThumbnailTable.dropTables(db);
        ReaderBlogTable.dropTables(db);*/
    }
    public ReaderDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }
    public static SQLiteDatabase getWritableDb() {
        return getDatabase().getWritableDatabase();
    }
}
