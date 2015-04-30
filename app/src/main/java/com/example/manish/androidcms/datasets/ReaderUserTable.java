package com.example.manish.androidcms.datasets;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.example.manish.androidcms.models.ReaderUser;
import com.example.manish.androidcms.models.ReaderUserList;

import org.wordpress.android.util.SqlUtils;

/**
 * Created by Manish on 4/15/2015.
 */
public class ReaderUserTable {

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_users ("
                + "	user_id	        INTEGER PRIMARY KEY,"
                + " blog_id         INTEGER DEFAULT 0,"
                + "	user_name	    TEXT,"
                + "	display_name	TEXT COLLATE NOCASE,"
                + " url             TEXT,"
                + " profile_url     TEXT,"
                + " avatar_url      TEXT)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_users");
    }


    public static void addOrUpdateUser(ReaderUser user) {
        if (user==null)
            return;

        ReaderUserList users = new ReaderUserList();
        users.add(user);
        addOrUpdateUsers(users);
    }

    public static void createTables(ReaderUser user) {
        if (user==null)
            return;

        ReaderUserList users = new ReaderUserList();
        users.add(user);
        addOrUpdateUsers(users);
    }

    private static final String COLUMN_NAMES =
            " user_id,"       // 1
                    + " blog_id,"       // 2
                    + " user_name,"     // 3
                    + " display_name,"  // 4
                    + " url,"           // 5
                    + " profile_url,"   // 6
                    + " avatar_url";    // 7

    public static void addOrUpdateUsers(ReaderUserList users) {
        if (users==null || users.size()==0)
            return;

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        SQLiteStatement stmt = db.compileStatement("INSERT OR REPLACE INTO tbl_users (" + COLUMN_NAMES + ") VALUES (?1,?2,?3,?4,?5,?6,?7)");
        try {
            for (ReaderUser user: users) {
                stmt.bindLong  (1, user.userId);
                stmt.bindLong  (2, user.blogId);
                stmt.bindString(3, user.getUserName());
                stmt.bindString(4, user.getDisplayName());
                stmt.bindString(5, user.getUrl());
                stmt.bindString(6, user.getProfileUrl());
                stmt.bindString(7, user.getAvatarUrl());
                stmt.execute();
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }
}
