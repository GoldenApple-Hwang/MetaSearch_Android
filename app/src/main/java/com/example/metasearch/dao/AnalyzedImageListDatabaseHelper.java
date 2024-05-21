package com.example.metasearch.dao;

import static androidx.fragment.app.FragmentManager.TAG;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.SQLException;
import android.util.Log;

import java.util.ArrayList;

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
    private AnalyzedImageListDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

//    public static AnalyzedImageListDatabaseHelper getInstance(Context context) {
//        if (instance == null) {
//            instance = new AnalyzedImageListDatabaseHelper(context.getApplicationContext());
//        }
//        return instance;
//    }

    // 생성자를 private으로 설정하여 외부에서 인스턴스화 방지


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

    @SuppressLint("RestrictedApi")
    public void addImagePath(String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IMAGE_PATH, imagePath);

        try {
            long result = db.insertOrThrow(TABLE_NAME, null, values);
            if (result == -1) {
                Log.e(TAG, "Failed to insert image path: " + imagePath);
            } else {
                Log.d(TAG, "Image path inserted successfully: " + imagePath);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Error adding image path: " + imagePath, e);
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
    public ArrayList<String> loadAnalyzedImages() {
        ArrayList<String> analyzed_image_list = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NAME, new String[]{COLUMN_IMAGE_PATH}, null, null, null, null, null);
            if (cursor == null) {
                return new ArrayList<>(); // 또는 적절한 예외 처리
            }
            analyzed_image_list = new ArrayList<>();
            while (cursor.moveToNext()) {
                analyzed_image_list.add(cursor.getString(0));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return analyzed_image_list;
    }
}
