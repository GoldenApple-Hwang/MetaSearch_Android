package com.example.metasearch.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.SQLException;

public class AnalyzedImageListDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "image_analyzer.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "analyzed_images";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_IMAGE_PATH = "image_path";

    // 싱글톤 인스턴스
    private static AnalyzedImageListDatabaseHelper instance;

    // 싱글톤 인스턴스 생성 메소드
    public static synchronized AnalyzedImageListDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new AnalyzedImageListDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    // 생성자를 private으로 설정하여 외부에서 인스턴스화 방지
    private AnalyzedImageListDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_IMAGE_PATH + " TEXT UNIQUE NOT NULL)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void addImagePath(String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IMAGE_PATH, imagePath);

        try {
            db.insertOrThrow(TABLE_NAME, null, values);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    public void removeImagePath(String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_NAME, COLUMN_IMAGE_PATH + " = ?", new String[]{imagePath});
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }
}
