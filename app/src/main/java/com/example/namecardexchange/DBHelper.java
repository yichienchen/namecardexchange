package com.example.namecardexchange;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    final static String TB1="saves_card";

    private final static int VS=1;
    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, null, VS);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //接觸史
        String SQL1 = "CREATE TABLE IF NOT EXISTS "+ TB1 +
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT , " +
                "NAME TEXT NOT NULL, " +
                "PHONE TEXT , "+
                "EMAIL TEXT , "+
                "COMPANY TEXT , "+
                "POSITION TEXT , "+
                "OTHER TEXT  )";

        db.execSQL(SQL1);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String SQL = "DROP TABLE " + TB1;
        db.execSQL(SQL);
    }
}
