package com.example.metasearch.manager;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.metasearch.dao.AnalyzedImageListDatabaseHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ImageAnalyzeListManager {
    //분석된 이미지 리스트
    private ArrayList<String> analyzed_image_list;
    private ArrayList<String> addImagePaths; //추가되는 이미지에 대한 경로를 저장하는 리스트
    private ArrayList<String> deleteImagePaths; //삭제되는 이미지에 대한 경로를 저장하는 리스트
    private AnalyzedImageListDatabaseHelper dbHelper;
    private SQLiteDatabase database;
    //싱글톤 객체
    private static ImageAnalyzeListManager imageAnalyzeListObject;
    private ImageAnalyzeListManager(Context context){ // 싱글톤
        dbHelper = AnalyzedImageListDatabaseHelper.getInstance(context);
        database = dbHelper.getWritableDatabase();
        loadAnalyzedImages();
        Log.d("analyzed_image_list", analyzed_image_list.toString());
        addImagePaths = new ArrayList<>();
        deleteImagePaths = new ArrayList<>();
    }

    public static ImageAnalyzeListManager getInstance(Context context){ //객체 생성(싱글톤 구현)
        if(imageAnalyzeListObject == null){
            imageAnalyzeListObject = new ImageAnalyzeListManager(context);
        }
        return imageAnalyzeListObject;
    }
    private void loadAnalyzedImages() {
        Cursor cursor = database.query("analyzed_images", new String[]{"image_path"}, null, null, null, null, null);
        analyzed_image_list = new ArrayList<>();
        while (cursor.moveToNext()) {
            analyzed_image_list.add(cursor.getString(0));
        }
        cursor.close();
    }
    public void addImagePath(String imagePath) {
        if (!analyzed_image_list.contains(imagePath)) {
            analyzed_image_list.add(imagePath);
            addImagePaths.add(imagePath);
            Log.d(TAG, "Added image path: " + imagePath);
            // Update database
            dbHelper.addImagePath(imagePath);
        }
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
        Iterator<String> iterator = analyzed_image_list.iterator();
        while (iterator.hasNext()) {
            String analyze_image_path = iterator.next();
            if(!imagesPaths.contains(analyze_image_path)) { //갤러리 상에 존재하지 않는 이미지 경로라면
                Log.d(TAG,"삭제할 이미지 찾음");
                //compare_image_list.put(analyze_image_path, "delete"); //삭제해야하는 이미지라는 것을 표시
//                deleteImagePaths.add(analyze_image_path); //삭제해야하는 이미지 경로 리스트에 추가


                iterator.remove(); //이미지 분석 리스트에서 해당 이미지 경로 요소 삭제
                removeImagePath(analyze_image_path);
            }
        }
        return deleteImagePaths;
    }
    public void removeImagePath(String imagePath) {
//        if (analyzed_image_list.contains(imagePath)) {
            deleteImagePaths.add(imagePath);
            Log.d(TAG, "Removed image path: " + imagePath);
            // Update database
            dbHelper.removeImagePath(imagePath);
//        }
    }
    //추가 분석할 이미지가 있는지 찾음
    public ArrayList<String> checkAddImagePath(ArrayList<String> imagesPaths){
        for (String imagePath : imagesPaths) { //이미지 경로를 하나씩 가져와서 갤러리 이미지 업로드 함수를 호출
            Log.d(TAG,"imagePath : "+imagePath); //-> ex) /storage/emulated/0/DCIM/20240322_200556.jpg
//            if(!analyzed_image_list.contains(imagePath)){ //갤러리에는 있으나 , 이미지 분석 리스트에는 존재하지 않는 경우, 추가
//                analyzed_image_list.add(imagePath);
                addImagePath(imagePath);
                Log.d(TAG,"추가함 imagePath : "+imagePath);
                //compare_image_list.put(imagePath,"add"); //추가 분석해야할 이미지라는 표시
//                addImagePaths.add(imagePath); //추가해야하는 이미지 경로 리스트에 추가
//            }
        }
        return  addImagePaths;
    }

}
