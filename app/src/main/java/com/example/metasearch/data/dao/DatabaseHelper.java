package com.example.metasearch.data.dao;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.ParcelFileDescriptor;
import android.provider.CallLog;
import android.util.Log;

import com.example.metasearch.data.model.Person;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {
    private final Context context;
    static final String TABLE_NAME = "Faces";
    private static final int DATABASE_VERSION = 4; // 데이터베이스 버전 번호 증가
    private static final String COLUMN_IMAGE = "IMAGE"; // 이미지 컬럼
    private static final String COLUMN_INPUTNAME = "INPUTNAME"; // 인물 이름 컬럼
    private static final String COLUMN_NAME = "NAME"; // 이미지 이름 컬럼
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
    private DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, DATABASE_VERSION);
        this.context = context;
        Log.d(TAG, "DataBaseHelper 생성자 호출");
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(TAG,"Table create");
        String createQuery = "CREATE TABLE " + TABLE_NAME +
                "( ID INTEGER PRIMARY KEY AUTOINCREMENT, " // 프라이머리 키 추가
                + "NAME TEXT NOT NULL, " // 사진 이름(예: person1.png, person2.jpg, ...)
                + "INPUTNAME TEXT, " // 인물 이름(기본 값은 인물1, 인물2, ...)
                + "PHONENUMBER TEXT, " // 인물 전화번호
                + "IMAGE BLOB," // 사진 데이터(바이트 배열)
                + "HOMEDISPLAY INTEGER DEFAULT 0, " // 홈 화면 표시 여부(= 내가 좋아하는 사람 리스트)
                + "THUMBNAIL_IMAGE BLOB);"; // 썸네일 이미지 데이터(바이트 배열)
        sqLiteDatabase.execSQL(createQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion){
        Log.d(TAG, "Table onUpgrade");

        if (oldVersion < 2) {  // 예전 버전이 2보다 작을 때 업그레이드 로직 실행
            sqLiteDatabase.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN INPUTNAME TEXT;");
        }
        if (oldVersion < 3) {
            sqLiteDatabase.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN HOME_DISPLAY INTEGER DEFAULT 0;");
        }
        if (oldVersion < 4) {
            sqLiteDatabase.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN THUMBNAIL_IMAGE BLOB;");
        }
    }

    // 테이블의 행의 개수를 반환하는 함수
    public int getRowCount(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String countQuery = "SELECT * FROM " + tableName;
        Cursor cursor = db.rawQuery(countQuery, null);
        int rowCount = cursor.getCount();
        cursor.close();

        return rowCount;
    }

    public static byte[] getBytes(Bitmap bitmap) { // 이미지를 바이트 배열로 변환하는 예시 코드
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        // 압축 품질 변경
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    public static Bitmap getImage(byte[] image) { // 바이트 배열을 Bitmap으로 변환하는 예시 코드
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
    public boolean isNameExists(String newName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{"ID"}, "INPUTNAME = ?", new String[]{newName}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    //이미지 이름과 input이름이 서로 다르면 해쉬맵에 추가하여 반환함
    public Map<String, String> getMismatchedImageInputNames() {
        SQLiteDatabase db = this.getReadableDatabase();
        Map<String, String> mismatchMap = new HashMap<>();

        Cursor cursor = null;
        try {
            // 모든 행을 조회
            String query = "SELECT NAME, INPUTNAME FROM "+TABLE_NAME;
            cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex("NAME"));
                    @SuppressLint("Range") String inputName = cursor.getString(cursor.getColumnIndex("INPUTNAME"));

                    // IMAGE와 INPUTNAME이 다른 경우 맵에 추가
                    if (!name.equals(inputName)) {
                        mismatchMap.put(name, inputName);
                    }
                } while (cursor.moveToNext());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return mismatchMap;
    }
    public boolean insertImage(String name, byte[] imageBytes) {
        Log.d(TAG, "이미지 추가함");
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("NAME", name);
        values.put("INPUTNAME", name); // 인물 이름 기본 값 인물1, 인물2, ...
        values.put("IMAGE", imageBytes); // 이미지 바이트
        values.put("PHONENUMBER", ""); // 휴대전화 번호 기본값 ""
        values.put("HOMEDISPLAY", 0); // 홈 화면 표시 여부 기본값 0
        long result = db.insert(TABLE_NAME, null, values);
        db.close(); // 데이터베이스 사용 후 닫기

        return result != -1;
    }
    public List<byte[]> getImagesByName(String name) {
        List<byte[]> images = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{"IMAGE"}, "INPUTNAME = ?", new String[]{name}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") byte[] image = cursor.getBlob(cursor.getColumnIndex("IMAGE"));
                images.add(image);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return images;
    }
    public Map<String, byte[]> getAllImages() {
        Map<String, byte[]> imagesMap = new HashMap<>();
        // 데이터베이스에서 모든 이미지 이름을 선택하는 쿼리
        String selectQuery = "SELECT " + "ID" + ", " + COLUMN_NAME + " FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            int idColumnIndex = cursor.getColumnIndex("ID"); // ID 컬럼 인덱스
            int nameColumnIndex = cursor.getColumnIndex(COLUMN_NAME); // 이름 컬럼 인덱스

            do {
                // 각 컬럼에서 데이터를 읽음
                int personId = cursor.getInt(idColumnIndex);
                String imageName = cursor.getString(nameColumnIndex);
                byte[] imageData = getImageData(personId);

                // 읽은 데이터를 HashMap에 추가
                imagesMap.put(imageName, imageData);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        // 이미지 데이터가 담긴 HashMap 반환
        return imagesMap;
    }
    // inputname을 통해 해당 컬럼 삭제
    public void deletePersonByName(String inputName) {
        SQLiteDatabase db = this.getWritableDatabase();
        // inputname을 기준으로 해당 행을 삭제
        int result = db.delete(TABLE_NAME, "INPUTNAME = ?", new String[]{inputName});
        db.close(); // 데이터베이스 사용 후 닫기
    }
    public String getPhoneNumberById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String phoneNumber = "";

        // 쿼리에서 ID를 기준으로 전화번호를 조회합니다.
        String query = "SELECT " + COLUMN_PHONENUMBER + " FROM " + TABLE_NAME + " WHERE ID = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});

        if (cursor.moveToFirst()) {
            phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONENUMBER));
        }

        cursor.close();
        db.close();

        return phoneNumber;
    }
    public Person getPersonById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT ID, NAME, INPUTNAME, PHONENUMBER, IMAGE, HOMEDISPLAY, THUMBNAIL_IMAGE FROM " + TABLE_NAME + " WHERE ID = ?", new String[]{String.valueOf(id)});

        if (cursor.moveToFirst()) {
            @SuppressLint("Range") int personId = cursor.getInt(cursor.getColumnIndex("ID"));
            @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex("NAME"));
            @SuppressLint("Range") String inputName = cursor.getString(cursor.getColumnIndex("INPUTNAME"));
            @SuppressLint("Range") String phoneNumber = cursor.getString(cursor.getColumnIndex("PHONENUMBER"));
            @SuppressLint("Range") byte[] image = cursor.getBlob(cursor.getColumnIndex("IMAGE"));
            @SuppressLint("Range") boolean homeDisplay = cursor.getInt(cursor.getColumnIndex("HOMEDISPLAY")) == 1;
            @SuppressLint("Range") byte[] thumbnailImage = cursor.getBlob(cursor.getColumnIndex("THUMBNAIL_IMAGE"));

            Person person = new Person(personId, name, image);
            person.setInputName(inputName);
            person.setPhone(phoneNumber);
            person.setHomeDisplay(homeDisplay);
            person.setThumbnailImage(thumbnailImage);

            cursor.close();
            db.close();
            return person;
        }

        cursor.close();
        db.close();
        return null;
    }

    // 이름이 같은 모든 사람의 썸네일 이미지를 변경하는 메서드
    public boolean updateThumbnailImageByName(String name, byte[] thumbnailImageBytes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("THUMBNAIL_IMAGE", thumbnailImageBytes);

        int result = db.update(TABLE_NAME, values, "INPUTNAME = ?", new String[]{name});
        db.close();

        return result != -1;
    }

    // 이름이 같은 모든 사람의 인물 정보(이름, 전화 번호, 내가 좋아하는 사람 여부) 변경하는 메서드
    public boolean updatePersonByName(String oldName, String newName, String newPhoneNumber, boolean homeDisplay) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("INPUTNAME", newName);
        values.put("PHONENUMBER", newPhoneNumber);
        values.put("HOMEDISPLAY", homeDisplay ? 1 : 0);

        String selection = "INPUTNAME = ?";
        String[] selectionArgs = { oldName };

        db.beginTransaction();
        try {
            int result = db.update(TABLE_NAME, values, selection, selectionArgs);
            db.setTransactionSuccessful();
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }
    // 전화 기록을 가져오는 메서드
    @SuppressLint("Range")
    private Map<String, Long> getCallDurations() {
        Map<String, Long> callLogDuration = new HashMap<>();
        Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                long duration = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DURATION));
                number = normalizePhoneNumber(number);
                callLogDuration.put(number, callLogDuration.getOrDefault(number, 0L) + duration);
            }
            cursor.close();
        }
        return callLogDuration;
    }

    // 전화번호 정규화 메서드
    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replaceAll("[^0-9]", "");
    }
    public List<Person> getUniquePersons() {
        List<Person> people = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Map<String, Long> callDurations = getCallDurations(); // 전화 기록 가져오기

        Cursor cursor = db.rawQuery("SELECT ID, NAME, INPUTNAME, PHONENUMBER, IMAGE, HOMEDISPLAY, THUMBNAIL_IMAGE FROM " + TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            Set<String> uniqueNames = new HashSet<>();
            do {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex("ID"));
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex("NAME"));
                @SuppressLint("Range") String inputName = cursor.getString(cursor.getColumnIndex("INPUTNAME"));
                @SuppressLint("Range") String phoneNumber = cursor.getString(cursor.getColumnIndex("PHONENUMBER"));
                @SuppressLint("Range") byte[] image = cursor.getBlob(cursor.getColumnIndex("IMAGE"));
                @SuppressLint("Range") boolean homeDisplay = cursor.getInt(cursor.getColumnIndex("HOMEDISPLAY")) == 1;
                @SuppressLint("Range") byte[] thumbnailImage = cursor.getBlob(cursor.getColumnIndex("THUMBNAIL_IMAGE"));


                // 중복된 이름 제거
                if (uniqueNames.add(inputName)) {
                    Person person = new Person(id, name, image);
                    person.setInputName(inputName);
                    person.setPhone(phoneNumber);
                    person.setHomeDisplay(homeDisplay);
                    person.setThumbnailImage(thumbnailImage);

                    // 통화량 설정
                    long totalDuration = callDurations.getOrDefault(phoneNumber, 0L);
                    person.setTotalDuration(totalDuration);

                    people.add(person);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return people;
    }


    public boolean getHomeDisplayById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean homeDisplay = false;

        String query = "SELECT HOMEDISPLAY FROM " + TABLE_NAME + " WHERE ID = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});

        if (cursor.moveToFirst()) {
            homeDisplay = cursor.getInt(cursor.getColumnIndexOrThrow("HOMEDISPLAY")) == 1;
        }

        cursor.close();
        db.close();

        return homeDisplay;
    }

    private byte[] getImageData(int personId) {
        SQLiteDatabase db = this.getReadableDatabase();
        SQLiteStatement statement = db.compileStatement("SELECT IMAGE FROM " + TABLE_NAME + " WHERE ID = ?");
        statement.bindLong(1, personId);
        byte[] buffer = new byte[2048]; // 버퍼 사이즈 설정
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            ParcelFileDescriptor pfd = statement.simpleQueryForBlobFileDescriptor();
            if (pfd != null) {
                FileInputStream inputStream = new FileInputStream(pfd.getFileDescriptor());
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                pfd.close();
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            statement.close();
        }
    }
    @SuppressLint("Range")
    public String getInputNameByImageName(String imageName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String inputName = null;

        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_INPUTNAME}, "NAME = ?", new String[]{imageName}, null, null, null);

        if (cursor.moveToFirst()) {
            inputName = cursor.getString(cursor.getColumnIndex(COLUMN_INPUTNAME));
        }
        cursor.close();
        db.close();
        return inputName;
    }
}