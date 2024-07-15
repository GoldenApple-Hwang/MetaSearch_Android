package com.example.metasearch.manager;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.metasearch.data.dao.AnalyzedImageListDatabaseHelper;

import java.util.ArrayList;
import java.util.Iterator;

public class ImageAnalyzeListManager {
    private ArrayList<String> analyzed_image_list;    //분석된 이미지 리스트
    private ArrayList<String> addImagePaths; //추가되는 이미지에 대한 경로를 저장하는 리스트
    private ArrayList<String> deleteImagePaths; //삭제되는 이미지에 대한 경로를 저장하는 리스트
    private AnalyzedImageListDatabaseHelper analyzedImageListDatabaseHelper;
    private SQLiteDatabase database;
    private static final String TABLE_NAME = "analyzed_images";
    private static final String COLUMN_IMAGE_PATH = "image_path";


    //싱글톤 객체
    private static ImageAnalyzeListManager imageAnalyzeListObject;
    private ImageAnalyzeListManager(Context context){ // 싱글톤
        analyzedImageListDatabaseHelper = AnalyzedImageListDatabaseHelper.getInstance(context);
        database = analyzedImageListDatabaseHelper.getWritableDatabase();
        //loadAnalyzedImages();
//        Log.d("analyzed_image_list", analyzed_image_list.toString());
        addImagePaths = new ArrayList<>(); //추가 이미지 경로를 저장하는 리스트
        deleteImagePaths = new ArrayList<>(); //삭제 이미지 경로를 저장하는 리스트
    }

    public static ImageAnalyzeListManager getInstance(Context context){ //객체 생성(싱글톤 구현)
        if(imageAnalyzeListObject == null){
            imageAnalyzeListObject = new ImageAnalyzeListManager(context);
        }
        return imageAnalyzeListObject;
    }

    //데이터베이스에 있는 리스트 가져옴
//    private ArrayList<String> loadAnalyzedImages() {
//        Cursor cursor = database.query(TABLE_NAME, new String[]{COLUMN_IMAGE_PATH}, null, null, null, null, null);
//        analyzed_image_list = new ArrayList<>();
//        while (cursor.moveToNext()) {
//            analyzed_image_list.add(cursor.getString(0));
//        }
//        cursor.close();
//        return analyzed_image_list;
//    }
    private ArrayList<String> loadAnalyzedImages() {
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, new String[]{COLUMN_IMAGE_PATH}, null, null, null, null, null);
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


    // analyzed_image_list 추가 & addImagePaths에 추가 & db에 추가
    public ArrayList<String> addImagePath(String imagePath,ArrayList<String> analyzed_image_list) {
        analyzed_image_list.add(imagePath); //analyzed_image_list에 추가
        addImagePaths.add(imagePath); //addImagePaths에 추가
        Log.d(TAG, "Added image path: " + imagePath);
        // Update database
        analyzedImageListDatabaseHelper.addImagePath(imagePath); //db에 추가


        return  addImagePaths;
    }

    //deleteImagePaths에 추가 & db에서 삭제
    public ArrayList<String> removeImagePath(String imagePath) {
//        if (analyzed_image_list.contains(imagePath)) {
        deleteImagePaths.add(imagePath);
        Log.d(TAG, "Removed image path: " + imagePath);
        analyzedImageListDatabaseHelper.removeImagePath(imagePath); //db에서 해당 이미지 경로 삭제

        return deleteImagePaths;
    }


    //추가, 삭제 정보 가지고 있는 리스트 정보 삭제
    public void clearAddDeleteImageList(){
        Log.d(TAG,"삭제 전 추가 이미지 리스트 사이즈 :"+addImagePaths.size());
        addImagePaths.clear();
        deleteImagePaths.clear();
        Log.d(TAG,"삭제 후 추가 이미지 리스트 사이즈 : "+addImagePaths.size());
    }

    //삭제해야하는 이미지 경로 리스트 반환
    public ArrayList<String> checkDeleteImagePath(ArrayList<String> imagesPaths){
        Log.d(TAG,"checkDeleteImagePath 함수에 들어옴");

        //데이타베이스에 있는 리스트 가져옴
        //analyzed_image_list = loadAnalyzedImages();
        analyzed_image_list = analyzedImageListDatabaseHelper.loadAnalyzedImages();
        Log.d(TAG,"analyzed_image_list 사이즈 : "+analyzed_image_list.size());


        Iterator<String> iterator = analyzed_image_list.iterator();
        while (iterator.hasNext()) {
            String analyze_image_path = iterator.next();
            if(!imagesPaths.contains(analyze_image_path)) { //갤러리 상에 존재하지 않는 이미지 경로라면
                Log.d(TAG,"삭제할 이미지 찾음");
                //compare_image_list.put(analyze_image_path, "delete"); //삭제해야하는 이미지라는 것을 표시
//                deleteImagePaths.add(analyze_image_path); //삭제해야하는 이미지 경로 리스트에 추가
                deleteImagePaths = removeImagePath(analyze_image_path); //deleteImagePaths에 추가 및 db에서 해당 데이터 삭제
                iterator.remove(); //이미지 분석 리스트에서 해당 이미지 경로 요소 삭제
            }
        }
        return deleteImagePaths;
    }

    //추가 분석할 이미지가 있는지 찾음
    public ArrayList<String> checkAddImagePath(ArrayList<String> imagesPaths){
        Log.d(TAG,"checkAddImagePath 함수에 들어옴");
        //데이타베이스에 있는 리스트 가져옴
        //analyzed_image_list = loadAnalyzedImages();
        analyzed_image_list = analyzedImageListDatabaseHelper.loadAnalyzedImages();
        Log.d(TAG,"analyzed_image_list 사이즈 : "+analyzed_image_list.size());

        for (String imagePath : imagesPaths) { //이미지 경로를 하나씩 가져와서 갤러리 이미지 업로드 함수를 호출
            Log.d(TAG,"imagePath : "+imagePath); //-> ex) /storage/emulated/0/DCIM/20240322_200556.jpg
            // 이미지 경로의 확장자 확인 //png,jpg,jpeg면 추가 이미지로 처리함
            if (imagePath.toLowerCase().endsWith(".png") ||
                    imagePath.toLowerCase().endsWith(".jpg") ||
                    imagePath.toLowerCase().endsWith(".jpeg")) {

                if(!analyzed_image_list.contains(imagePath)){ //이미지 분석에서 해당 이미지 경로가 없다면 추가
                    //analyze_image_list에 추가 & addImagePaths에 추가 & db에 추가
                    addImagePaths = addImagePath(imagePath,analyzed_image_list);
                    Log.d(TAG, "추가함 imagePath : " + imagePath);
                }
            }
        }
        return  addImagePaths;
    }

    //네트워크 에러로 인해 분석되지 않은 이미지 경로를 분석된 이미지 리스트에서 삭재하는 함수
    public void delete_fail_image_analyze(String imagePath){
        //해당 이미지는 분석되지 않았다고 처리함
        analyzed_image_list.remove(imagePath);
        //database에서도 삭제함
        analyzedImageListDatabaseHelper.removeImagePath(imagePath);

    }

}
