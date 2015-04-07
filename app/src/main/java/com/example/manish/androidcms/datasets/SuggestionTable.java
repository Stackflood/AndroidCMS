package com.example.manish.androidcms.datasets;

import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.util.AppLog;

/**
 * Created by Manish on 4/1/2015.
 */
public class SuggestionTable {
    private static final String SUGGESTIONS_TABLE = "suggestions";

    public static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + SUGGESTIONS_TABLE + " ("
                + "    site_id              INTEGER DEFAULT 0,"
                + "    user_login           TEXT,"
                + "    display_name         TEXT,"
                + "    image_url            TEXT,"
                + "    taxonomy             TEXT,"
                + "    PRIMARY KEY (user_login)"
                + " );");
    }

    private static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + SUGGESTIONS_TABLE);
    }

    public static void reset(SQLiteDatabase db)
    {
        AppLog.i(AppLog.T.SUGGESTION, "resetting suggestion table");
        dropTables(db);
        createTables(db);
    }
}
