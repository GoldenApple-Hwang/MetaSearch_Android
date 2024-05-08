package com.example.metasearch.dao;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.example.metasearch.model.Person;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    static final String TABLE_NAME = "Faces";
    private static final String COLUMN_IMAGE = "IMAGE"; // 이미지 컬럼
    private static final String COLUMN_NAME = "NAME"; // 이미지 이름 컬럼
    private static final String COLUMN_USERNAME = "USERNAME"; // 유저가 입력한 이름 컬럼
    private static final String COLUMN_PHONENUMBER = "PHONENUMBER"; // 전화번호 컬럼
    private static DatabaseHelper instance;

    // DatabaseHelper 싱글톤 생성자
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext(), "FACEIMAGE.db", null, 1);
        }
        return instance;
    }
    // 싱글톤으로 만들기 위해 private으로 변경
    private DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
        super(context, name, factory, version);
        Log.d(TAG,"DataBaseHelper 생성자 호출");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase){
        Log.d(TAG,"Table create");
        String createQuery = "CREATE TABLE " + TABLE_NAME +
                "( ID INTEGER PRIMARY KEY AUTOINCREMENT, " // 프라이머리 키 추가
                + "NAME TEXT NOT NULL, "
                + "PHONENUMBER TEXT, "
                + "USERNAME TEXT, "
                + "IMAGE BLOB, " // 이미지 컬럼 추가
                + "IS_DELETE INTEGER DEFAULT 1);"; // IS_VERIFIED 컬럼 추가, BOOLEAN 대신 INTEGER 사용
        sqLiteDatabase.execSQL(createQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion){
        Log.d(TAG, "Table onUpgrade");
        // 테이블 재정의하기 위해 현재의 테이블 삭제
//        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
//        onCreate(sqLiteDatabase); // 새로운 테이블 생성
    }

    public static byte[] getBytes(Bitmap bitmap){ // 이미지를 바이트 배열로 변환하는 예시 코드
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 0, stream);
        return stream.toByteArray();
    }

    public static Bitmap getImage(byte[] image){ // 바이트 배열을 Bitmap으로 변환하는 예시 코드
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    // Drawable 리소스를 바이트 배열로 변환하는 메소드
    public byte[] drawableToBytes(Context context, int drawableId) throws IOException {
        Resources resources = context.getResources();
        InputStream inputStream = resources.openRawResource(drawableId);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int size;
        byte[] buffer = new byte[1024];
        while ((size = inputStream.read(buffer, 0, 1024)) >= 0) {
            outputStream.write(buffer, 0, size);
        }
        inputStream.close();
        return outputStream.toByteArray();
    }
    public boolean insertImage(String name ,byte[] imageBytes) {
        Log.d(TAG,"이미지 추가함");
        //userNum +=1; //한 명 추가
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("NAME", name);
        values.put("USERNAME",""); //'인물1'과 같이 나타냄
        values.put("IMAGE", imageBytes); //이미지 바이트
        values.put("PHONENUMBER",""); //휴대전화 번호 ""(기본값)
        //values.put(DBHelper.COLUMN_IMAGE, imageBytes);
        long result = db.insert(TABLE_NAME, null, values);
        db.close(); // 데이터베이스 사용 후 닫기

        return result != -1;
        // database.insert(DBHelper.TABLE_NAME, null, values);
    }

    //유저가 이름을 입력하면 USERNAME을 해당 이름을 변경
    public boolean updateUserName(String beforeUserName, String afterUserName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("USERNAME", afterUserName); // 변경할 새로운 사용자 이름 설정

        // USERNAME 컬럼이 beforeUserName과 일치하는 행을 찾아서 update 실행
        String selection = "USERNAME = ?";
        String[] selectionArgs = { beforeUserName };

        int result = db.update(TABLE_NAME, values, selection, selectionArgs);

        db.close(); // 데이터베이스 사용 후 닫기

        return result != -1; // 하나 이상의 행이 변경되었다면 true, 아니면 false 반환
    }

    //이미지 이름을 통해 해당 컬럼 삭제
    public boolean deleteImage(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        // 이름을 기준으로 해당 이미지를 삭제합니다.
        int result = db.delete(TABLE_NAME, "NAME = ?", new String[]{name});
        db.close(); // 데이터베이스 사용 후 닫기

        return result != -1; // 삭제된 행의 수가 0보다 크면 true를 반환
    }
    // 데이터베이스에서 모든 이미지와 이름을 선택하여 반환
    public Map<String, byte[]> getAllImages() {
        Map<String, byte[]> imagesMap = new HashMap<>();
        // 데이터베이스에서 모든 이미지와 이름을 선택하는 쿼리
        String selectQuery = "SELECT * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(COLUMN_NAME); // 이름 컬럼 인덱스
            int imageColumnIndex = cursor.getColumnIndex(COLUMN_IMAGE); // 이미지 컬럼 인덱스
            do {
                // 각 컬럼에서 데이터를 읽음
                String imageName = cursor.getString(nameColumnIndex);
                byte[] imageData = cursor.getBlob(imageColumnIndex);
                // 읽은 데이터를 HashMap에 추가
                imagesMap.put(imageName, imageData);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        // 이미지 데이터가 담긴 HashMap 반환
        return imagesMap;
    }
    // 데이터베이스에서 모든 행의 정보(사진 이름, 사진 정보, 인물 이름)를 가져와서 Person 데이터 모델 형식으로 반환
    public List<Person> getAllPerson() {
        List<Person> people = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        int imageNameColumnIndex = cursor.getColumnIndex("NAME");
        int usernameColumnIndex = cursor.getColumnIndex("USERNAME");
        int imageColumnIndex = cursor.getColumnIndex("IMAGE");

        if (cursor.moveToFirst()) {
            do {
                String imageName = cursor.getString(imageNameColumnIndex); // 사진 이름
                String username = cursor.getString(usernameColumnIndex); // 인물 이름
                byte[] imageData = cursor.getBlob(imageColumnIndex); // 사진 데이터
                if (imageName != null && username != null && imageData != null) {
                    people.add(new Person(imageName,username,imageData));
                    Log.d(TAG, "Loaded byte data for username: " + username);
                } else {
                    Log.d(TAG, "Null value found for username or image data");
                }
            } while (cursor.moveToNext());
        } else {
            Log.d(TAG, "No data found in the database");
        }
        cursor.close();
        db.close();
        return people;
    }
    public ArrayList<byte[]> getImageData(){
        ArrayList<byte[]> images = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_NAME, null);
        if(cursor.moveToFirst()){
            do{
                byte[] imgByte = cursor.getBlob(0);
                images.add(imgByte);
            }while(cursor.moveToNext());
        }
        cursor.close();
        return images;
    }

}
