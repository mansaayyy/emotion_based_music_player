package com.microsoft.projectoxford.emotionsample;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;


public class DatabaseAccess {

    private SQLiteOpenHelper openHelper;
    private SQLiteDatabase database;
    private static DatabaseAccess instance;

    private DatabaseAccess(Context context) {
        this.openHelper = new DatabaseOpenHelper(context);
    }


    public static DatabaseAccess getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseAccess(context);
        }
        return instance;
    }


    public void open() {
        this.database = openHelper.getWritableDatabase();
    }


    public void close() {
        if (database != null) {
            this.database.close();
        }
    }


    public List<String> getSongs() {
        List<String> list = new ArrayList<>();

        if(RecognizeActivity.select.equals("surprise")) {
            Cursor cursor = database.rawQuery("SELECT * FROM tbl where emotion='angry'", null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                list.add(cursor.getString(0));
                cursor.moveToNext();
            }
            cursor.close();

       }
        else if(RecognizeActivity.select.equals("happy")) {
            Cursor cursor = database.rawQuery("SELECT * FROM tbl where emotion='happy'", null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                list.add(cursor.getString(0));
                cursor.moveToNext();
            }
            cursor.close();

        }
        else if(RecognizeActivity.select.equals("sad")) {
            Cursor cursor = database.rawQuery("SELECT * FROM tbl where emotion='sad'", null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                list.add(cursor.getString(0));
                 cursor.moveToNext();
            }
            cursor.close();

        }
        else if(RecognizeActivity.select.equals("neutral")) {
            Cursor cursor = database.rawQuery("SELECT Song,Url FROM tbl where emotion='neutral'", null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                list.add(cursor.getString(0));
                cursor.moveToNext();
            }
            cursor.close();
        }

        else if(RecognizeActivity.select.equals("anger")) {
            Cursor cursor = database.rawQuery("SELECT * FROM tbl where emotion='angry'", null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                list.add(cursor.getString(0));
                cursor.moveToNext();
            }
            cursor.close();

        }
        else
        {
            Cursor cursor = database.rawQuery("SELECT * FROM tbl", null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                list.add(cursor.getString(0));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return list;
    }

}
