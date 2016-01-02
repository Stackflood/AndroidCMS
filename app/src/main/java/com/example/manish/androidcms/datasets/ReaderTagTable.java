package com.example.manish.androidcms.datasets;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import com.example.manish.androidcms.models.ReaderTag;
import com.example.manish.androidcms.models.ReaderTagList;
import com.example.manish.androidcms.models.ReaderTagType;
import com.example.manish.androidcms.ui.reader.ReaderConstants;
import com.example.manish.androidcms.util.DateTimeUtils;

import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.StringUtils;

import java.util.Date;

/**
 *  tbl_tags stores the list of tags the user subscribed to or has by default
 *  tbl_tags_recommended stores the list of recommended tags returned by the api
 */
public class ReaderTagTable {


    /*
     * determine whether the passed tag should be auto-updated based on when it was last updated
     */
    public static boolean shouldAutoUpdateTag(ReaderTag tag) {
        int minutes = minutesSinceLastUpdate(tag);
        if (minutes == NEVER_UPDATED) {
            return true;
        }
        return (minutes >= ReaderConstants.READER_AUTO_UPDATE_DELAY_MINUTES);
    }

    static ReaderTagList getAllTags() {
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_tags ORDER BY tag_name", null);
        try {
            ReaderTagList tagList = new ReaderTagList();
            if (c.moveToFirst()) {
                do {
                    tagList.add(getTagFromCursor(c));
                } while (c.moveToNext());
            }
            return tagList;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }



    private static final int NEVER_UPDATED = -1;
    private static int minutesSinceLastUpdate(ReaderTag tag) {
        if (tag == null) {
            return 0;
        }

        String updated = getTagLastUpdated(tag);
        if (TextUtils.isEmpty(updated)) {
            return NEVER_UPDATED;
        }

        Date dtUpdated = DateTimeUtils.iso8601ToJavaDate(updated);
        if (dtUpdated == null) {
            return 0;
        }

        Date dtNow = new Date();
        return DateTimeUtils.minutesBetween(dtUpdated, dtNow);
    }

    public static String getTagLastUpdated(ReaderTag tag) {
        if (tag == null) {
            return "";
        }
        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(),
                "SELECT date_updated FROM tbl_tags WHERE tag_name=? AND tag_type=?",
                args);
    }


    public static String getEndpointForTag(ReaderTag tag) {
        if (tag == null) {
            return null;
        }
        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(),
                "SELECT endpoint FROM tbl_tags WHERE tag_name=? AND tag_type=?",
                args);
    }

    public static ReaderTagList getDefaultTags() {
        return getTagsOfType(ReaderTagType.DEFAULT);
    }

    private static ReaderTagList getTagsOfType(ReaderTagType tagType) {
        String[] args = {Integer.toString(tagType.toInt())};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_tags WHERE tag_type=? ORDER BY tag_name", args);
        try {
            ReaderTagList tagList = new ReaderTagList();
            if (c.moveToFirst()) {
                do {
                    tagList.add(getTagFromCursor(c));
                } while (c.moveToNext());
            }
            return tagList;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }
    public static ReaderTagList getFollowedTags() {
        return getTagsOfType(ReaderTagType.FOLLOWED);
    }

    private static ReaderTag getTagFromCursor(Cursor c) {
        if (c == null) {
            throw new IllegalArgumentException("null tag cursor");
        }

        String tagName = c.getString(c.getColumnIndex("tag_name"));
        String endpoint = c.getString(c.getColumnIndex("endpoint"));
        ReaderTagType tagType = ReaderTagType.fromInt(c.getInt(c.getColumnIndex("tag_type")));

        return new ReaderTag(tagName, endpoint, tagType);
    }







    public static void setTagLastUpdated(ReaderTag tag) {
        if (tag == null) {
            return;
        }

        String date = DateTimeUtils.javaDateToIso8601(new Date());
        String sql = "UPDATE tbl_tags SET date_updated=?1 WHERE tag_name=?2 AND tag_type=?3";
        SQLiteStatement stmt = ReaderDatabase.getWritableDb().compileStatement(sql);
        try {
            stmt.bindString(1, date);
            stmt.bindString(2, tag.getTagName());
            stmt.bindLong  (3, tag.tagType.toInt());
            stmt.execute();
        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }
    /*
     * returns true if the passed tag exists, regardless of type
     */
    public static boolean tagExists(ReaderTag tag)
    {
        if(tag == null)
        {
            return false;
        }

        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                "SELECT 1 FROM tbl_tags WHERE tag_name=?1 AND tag_type=?2",
                args
                );
    }

    protected static void createTables(SQLiteDatabase db)
    {
        db.execSQL("CREATE TABLE tbl_tags ("
                + "	tag_name     TEXT COLLATE NOCASE,"
                + "    tag_type     INTEGER DEFAULT 0,"
                + "    endpoint     TEXT,"
                + " 	date_updated TEXT,"
                + "    PRIMARY KEY (tag_name, tag_type)"
                + ")");

        db.execSQL("CREATE TABLE tbl_tags_recommended ("
                + "	tag_name	TEXT COLLATE NOCASE,"
                + "    tag_type    INTEGER DEFAULT 0,"
                + "    endpoint    TEXT,"
                + "    PRIMARY KEY (tag_name, tag_type)"
                + ")");

    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_tags");
        db.execSQL("DROP TABLE IF EXISTS tbl_tags_recommended");
    }
}
