package com.example.metasearch.dao;

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

import com.example.metasearch.model.Person;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {
    private final Context context;
    static final String TABLE_NAME = "Faces";
    private static final int DATABASE_VERSION = 2; // 데이터베이스 버전 번호 증가
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
    public void onCreate(SQLiteDatabase sqLiteDatabase){
        Log.d(TAG,"Table create");
        String createQuery = "CREATE TABLE " + TABLE_NAME +
                "( ID INTEGER PRIMARY KEY AUTOINCREMENT, " // 프라이머리 키 추가
                + "NAME TEXT NOT NULL, "
                + "INPUTNAME TEXT, " // 인물 이름(기본 값은 인물1, 인물2, ...)
                + "PHONENUMBER TEXT, "
                + "IMAGE BLOB); "; // 이미지 컬럼 추가
        sqLiteDatabase.execSQL(createQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion){
        Log.d(TAG, "Table onUpgrade");

        if (oldVersion < 2) {  // 예전 버전이 2보다 작을 때 업그레이드 로직 실행
            sqLiteDatabase.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN INPUTNAME TEXT;");
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
        bitmap.compress(Bitmap.CompressFormat.JPEG, 0, stream);
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
    public boolean insertImage(String name ,byte[] imageBytes) {
        Log.d(TAG,"이미지 추가함");
        //userNum +=1; //한 명 추가
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("NAME", name);
        values.put("INPUTNAME", name); // 인물 이름(기본 값은 인물1, 인물2, ...)
        values.put("IMAGE", imageBytes); // 이미지 바이트
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
        values.put("NAME", afterUserName); // 변경할 새로운 사용자 이름 설정

        // USERNAME 컬럼이 beforeUserName과 일치하는 행을 찾아서 update 실행
        String selection = "NAME = ?";
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
//    public Map<String, byte[]> getAllImages() {
//        Map<String, byte[]> imagesMap = new HashMap<>();
//        // 데이터베이스에서 모든 이미지와 이름을 선택하는 쿼리
//        String selectQuery = "SELECT * FROM " + TABLE_NAME;
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(selectQuery, null);
//        if (cursor.moveToFirst()) {
//            int nameColumnIndex = cursor.getColumnIndex(COLUMN_NAME); // 이름 컬럼 인덱스
//            int imageColumnIndex = cursor.getColumnIndex(COLUMN_IMAGE); // 이미지 컬럼 인덱스
//            do {
//                // 각 컬럼에서 데이터를 읽음
//                String imageName = cursor.getString(nameColumnIndex);
//                byte[] imageData = cursor.getBlob(imageColumnIndex);
//                // 읽은 데이터를 HashMap에 추가
//                imagesMap.put(imageName, imageData);
//            } while (cursor.moveToNext());
//        }
//        cursor.close();
//        db.close();
//        // 이미지 데이터가 담긴 HashMap 반환
//        return imagesMap;
//    }
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
    public List<Person> getPersonsByCallDuration() {
        List<Person> persons = new ArrayList<>();
        Set<String> processedNames = new HashSet<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // 전화번호가 있는 인물만 선택하고 '나'라는 이름을 가진 인물은 제외
        String selection = "PHONENUMBER <> '' AND INPUTNAME <> '나'";
        Cursor personCursor = db.query(TABLE_NAME, new String[]{"ID", "NAME", "INPUTNAME", "PHONENUMBER"}, selection, null, null, null, null);

        if (personCursor.moveToFirst()) {
            do {
                @SuppressLint("Range") int id = personCursor.getInt(personCursor.getColumnIndex("ID"));
                @SuppressLint("Range") String name = personCursor.getString(personCursor.getColumnIndex("NAME"));
                @SuppressLint("Range") String inputName = personCursor.getString(personCursor.getColumnIndex("INPUTNAME"));
                @SuppressLint("Range") String phoneNumber = personCursor.getString(personCursor.getColumnIndex("PHONENUMBER"));
                //@SuppressLint("Range") byte[] image = personCursor.getBlob(personCursor.getColumnIndex("IMAGE"));
                byte[] image = getImageData(id);

                if (!processedNames.contains(name)) {
                    long totalDuration = getTotalCallDurationForNumber(phoneNumber);
                    Person person = new Person(id, name, image);
                    person.setInputName(inputName);
                    person.setPhone(phoneNumber);
                    person.setTotalDuration(totalDuration);
                    persons.add(person);
                    processedNames.add(name); // 이름을 처리된 이름으로 추가
                }
            } while (personCursor.moveToNext());
        }
        personCursor.close();
        db.close();

        // 통화 시간으로 리스트 정렬
        Collections.sort(persons, (p1, p2) -> Long.compare(p2.getTotalDuration(), p1.getTotalDuration()));
        return persons;
    }
    public String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replaceAll("[^0-9]", "");
    }

    @SuppressLint("Range")
    private long getTotalCallDurationForNumber(String phoneNumber) {
        long totalDuration = 0;
        phoneNumber = normalizePhoneNumber(phoneNumber); // 전화번호 정규화
        Log.d(TAG, "getTotalCallDurationForNumber - normalized phoneNumber: " + phoneNumber); // 로그 추가

        Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.DURATION, CallLog.Calls.NUMBER},
                CallLog.Calls.NUMBER + " LIKE ?",
                new String[]{"%" + phoneNumber + "%"},
                null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String logNumber = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                    long duration = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DURATION));
                    Log.d(TAG, "logNumber: " + logNumber + ", duration: " + duration); // 로그 추가
                    totalDuration += duration;
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "Cursor is empty for phoneNumber: " + phoneNumber); // 로그 추가
            }
            cursor.close();
        } else {
            Log.d(TAG, "Cursor is null for phoneNumber: " + phoneNumber); // 로그 추가
        }

        Log.d(TAG, "Total call duration for phoneNumber " + phoneNumber + ": " + totalDuration); // 로그 추가
        return totalDuration;
    }
    // inputname을 통해 해당 컬럼 삭제
    public void deletePersonByName(String inputName) {
        SQLiteDatabase db = this.getWritableDatabase();
        // inputname을 기준으로 해당 행을 삭제
        int result = db.delete(TABLE_NAME, "INPUTNAME = ?", new String[]{inputName});
        db.close(); // 데이터베이스 사용 후 닫기
    }
    public String getPhoneNumberByName(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        String phoneNumber = "";

        // 쿼리에서 이름을 기준으로 전화번호를 조회합니다.
        String query = "SELECT " + COLUMN_PHONENUMBER + " FROM " + TABLE_NAME + " WHERE " + COLUMN_INPUTNAME + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{name});

        if (cursor.moveToFirst()) {
            phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONENUMBER));
        }

        cursor.close();
        db.close();

        return phoneNumber;
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
        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_NAME, COLUMN_INPUTNAME, COLUMN_PHONENUMBER}, "ID = ?", new String[]{String.valueOf(id)}, null, null, null);
        Person person = null;

        if (cursor.moveToFirst()) {
            @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
            @SuppressLint("Range") String inputName = cursor.getString(cursor.getColumnIndex(COLUMN_INPUTNAME));
            //@SuppressLint("Range") byte[] image = cursor.getBlob(cursor.getColumnIndex(COLUMN_IMAGE));
            @SuppressLint("Range") String phoneNumber = cursor.getString(cursor.getColumnIndex(COLUMN_PHONENUMBER));
            byte[] image = getImageData(id); // 이미지 데이터 스트리밍을 통해 가져오기

            person = new Person(id, name, image);
            person.setInputName(inputName);
            person.setPhone(phoneNumber);
        }

        cursor.close();
        db.close();
        return person;
    }
    public boolean updatePersonByName(String oldName, String newName, String newPhoneNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_INPUTNAME, newName);
        values.put(COLUMN_PHONENUMBER, newPhoneNumber);

        String selection = COLUMN_INPUTNAME + " = ?";
        String[] selectionArgs = { oldName };

        // 디버그 로그 추가
        Log.d(TAG, "Updating person: oldName=" + oldName + ", newName=" + newName + ", newPhoneNumber=" + newPhoneNumber);

        db.beginTransaction();
        try {
            int result = db.update(TABLE_NAME, values, selection, selectionArgs);
            db.setTransactionSuccessful();

            // 업데이트된 행 수에 대한 로그 추가
            Log.d(TAG, "Updated rows: " + result);

            return result > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating person", e);
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public List<Person> getAllPersonExceptMe() {
        List<Person> people = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT ID, NAME, INPUTNAME, PHONENUMBER FROM " + TABLE_NAME + " WHERE INPUTNAME <> '나'", null);
        HashSet<String> seenNames = new HashSet<>(); // 중복 이름 추적을 위한 HashSet

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex("ID"));
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
                @SuppressLint("Range") String inputName = cursor.getString(cursor.getColumnIndex(COLUMN_INPUTNAME));
                @SuppressLint("Range") String phoneNumber = cursor.getString(cursor.getColumnIndex(COLUMN_PHONENUMBER));

                byte[] image = getImageData(id); // 이미지 데이터 스트리밍을 통해 가져오기

                if (!seenNames.contains(inputName)) {
                    long totalDuration = getTotalCallDurationForNumber(phoneNumber);
                    // 이미 처리한 이름이 아니면 추가
                    Person person = new Person(id, name, image);
                    person.setInputName(inputName);
                    person.setPhone(phoneNumber);
                    person.setTotalDuration(totalDuration); // 통화량 추가
                    people.add(person);
                    seenNames.add(inputName); // 처리된 이름을 추가
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return people;
    }

    /*
    '나'를 맨 앞에 리턴하고, 나머지는 일단 이름 중복없도록
    인물 리스트 반환
     */
//    public List<Person> getAllPerson() {
//        List<Person> people = new ArrayList<>();
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery("SELECT ID, NAME, INPUTNAME, PHONENUMBER  FROM " + TABLE_NAME, null);
//        HashSet<String> seenNames = new HashSet<>(); // 중복 이름 추적을 위한 HashSet
//        boolean isMyPersonAdded = false; // '나' 인물이 추가되었는지 여부
//
//        if (cursor.moveToFirst()) {
//            do {
//                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex("ID"));
//                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
//                @SuppressLint("Range") String inputName = cursor.getString(cursor.getColumnIndex(COLUMN_INPUTNAME));
//                @SuppressLint("Range") String phoneNumber = cursor.getString(cursor.getColumnIndex(COLUMN_PHONENUMBER));
//
//                byte[] image = getImageData(id); // 이미지 데이터 스트리밍을 통해 가져오기
//
//                if (inputName.equals("나")) {
//                    // '나' 인물이 이미 추가되었는지 확인
//                    if (!isMyPersonAdded) {
//                        // '나' 인물을 리스트에 추가
//                        Person myPerson = new Person(id, name, image);
//                        myPerson.setInputName(inputName);
//                        myPerson.setPhone(phoneNumber);
//                        people.add(0, myPerson); // '나' 인물을 리스트의 맨 앞에 추가
//                        isMyPersonAdded = true; // '나' 인물이 추가되었음을 표시
//                    }
//                } else if (!seenNames.contains(inputName)) {
//                    long totalDuration = getTotalCallDurationForNumber(phoneNumber);
//                    // 이미 처리한 이름이 아니면 추가
//                    Person person = new Person(id, name, image);
//                    person.setInputName(inputName);
//                    person.setPhone(phoneNumber);
//                    person.setTotalDuration(totalDuration); // 통화량 추가
//                    people.add(person);
//                    seenNames.add(inputName); // 처리된 이름을 추가
//                }
//            } while (cursor.moveToNext());
//        }
//        cursor.close();
//        db.close();
//
//        return people;
//    }
    public List<Person> getAllPerson() {
        List<Person> people = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT ID, NAME, INPUTNAME, PHONENUMBER, IMAGE FROM " + TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex("ID"));
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex("NAME"));
                @SuppressLint("Range") String inputName = cursor.getString(cursor.getColumnIndex("INPUTNAME"));
                @SuppressLint("Range") String phoneNumber = cursor.getString(cursor.getColumnIndex("PHONENUMBER"));
                @SuppressLint("Range") byte[] image = cursor.getBlob(cursor.getColumnIndex("IMAGE"));

                Person person = new Person(id, name, image);
                person.setInputName(inputName);
                person.setPhone(phoneNumber);
                people.add(person);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return people;
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
//    public ArrayList<byte[]> getImageData(){
//        ArrayList<byte[]> images = new ArrayList<>();
//        SQLiteDatabase db = this.getWritableDatabase();
//        Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_NAME, null);
//        if(cursor.moveToFirst()){
//            do{
//                byte[] imgByte = cursor.getBlob(0);
//                images.add(imgByte);
//            }while(cursor.moveToNext());
//        }
//        cursor.close();
//        return images;
//    }

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
    public Person getPersonByInputName(String inputName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_NAME, COLUMN_INPUTNAME, COLUMN_PHONENUMBER}, "INPUTNAME = ?", new String[]{inputName}, null, null, null);
        Person person = null;

        if (cursor.moveToFirst()) {
            @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex("ID"));
            @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
            @SuppressLint("Range") String phoneNumber = cursor.getString(cursor.getColumnIndex(COLUMN_PHONENUMBER));
            byte[] image = getImageData(id);

            person = new Person(id, name, image);
            person.setInputName(inputName);
            person.setPhone(phoneNumber);
        }

        cursor.close();
        db.close();
        return person;
    }

}