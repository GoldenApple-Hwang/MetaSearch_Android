package com.example.metasearch.manager;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.util.Log;

import com.example.metasearch.helper.HttpHelper;
import com.example.metasearch.model.response.PhotoResponse;
import com.example.metasearch.service.ApiService;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class WebRequestManager {
    private static final String Webserver_BASE_URL = "http://113.198.85.4"; // web 서버의 기본 url
    private static WebRequestManager webImageUploader;
    private Retrofit webRetrofit;
    private ApiService webService;

    private WebRequestManager(){
        //this.aiService = AIHttpService.getInstance(AIserver_BASE_URL);
        this.webService = HttpHelper.getInstance(Webserver_BASE_URL).getRetrofit().create(ApiService.class);
    }

    public ApiService getWebService(){
        return webService;
    }

    public static WebRequestManager getWebImageUploader(){
        Log.d(TAG,"AIImageUploaderController 함수 들어옴 객체 생성 or 반환");
        if(webImageUploader == null){
            webImageUploader = new WebRequestManager();

        }
        return webImageUploader;
    }

    //추가된 이미지 관해 서버에 전송
//    public void uploadAddGalleryImage(DatabaseHelper databaseHelper, ImageUploadService service, File imageFile, String source){
//        Log.d(TAG,"web의 uploadAddGalleryImage 안에 들어옴");
//        //List<CompletableFuture<Void>> futuresList = new ArrayList<>();
//        Log.d(TAG,"addImge 이름 : " + imageFile.getName());
//        RequestBody requestBody;
//        MultipartBody.Part imagePart;
//        //추가 이미지 분석이 필요한 이미지를 전달
//        requestBody = RequestBody.create(MediaType.parse("image"),imageFile);
//        //파일의 경로를 해당 이미지의 이름으로 설정함
//        imagePart = MultipartBody.Part.createFormData("addImage",imageFile.getName(),requestBody);
//
//        //이미지 출처 정보를 전송할 RequestBody 생성
//        RequestBody sourceBody = RequestBody.create(MediaType.parse("text/plain"),source); //DB이름과 어디에 저장되어야하는지에 관한 정보를 전달
//
//        //API 호출
//        Call<UploadResponse> call = service.uploadWebAddImage(imagePart,sourceBody); //이미지 업로드 API 호출
//        call.enqueue(new Callback<UploadResponse>() { //비동기
//            @Override
//            public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
//                if (response.isSuccessful()) {
//                    Log.e(TAG, "Image upload 성공: " + response.message());
//                }
//            }
//            @Override
//            public void onFailure(Call<UploadResponse> call, Throwable t) {
//                Log.e(TAG, "추가 이미지 업로드 실패함" + t.getMessage());
//            }
//        });
//    }
    public void uploadAddGalleryImage(ApiService service, File imageFile, String source){
        Log.d(TAG,"web의 uploadAddGalleryImage 안에 들어옴");

        Log.d(TAG,"addImge 이름 : " + imageFile.getName());
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", imageFile.getName(), requestBody);

        RequestBody sourceBody = RequestBody.create(MediaType.parse("text/plain"), source);

        Call<Void> call = service.uploadWebAddImage(imagePart, sourceBody);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.e(TAG, "Image upload 성공: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "추가 이미지 업로드 실패함" + t.getMessage());
            }
        });
    }




    // UI 업데이트를 위한 콜백 메서드
    public interface DetectedDataUploadCallbacks {
        void onDetectedDataUploadSuccess(PhotoResponse detectedObjects);
        void onDetectedDataUploadFailure(String message);
    }
    // Web Server로 Circle to Search 이미지 분석 결과 전송
    public void sendDetectedObjectsToAnotherServer(List<String> detectedObjects, String dbName, WebRequestManager.DetectedDataUploadCallbacks callbacks) {
        // Gson 인스턴스 생성
        Gson gson = new Gson();

        // detectedObjects와 dbName을 포함하는 Map 객체 생성
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("dbName", dbName);
        jsonMap.put("properties", detectedObjects);

        // Map 객체를 JSON 문자열로 변환
        String jsonObject = gson.toJson(jsonMap);

        // JSON 문자열을 바디로 사용하여 RequestBody 객체 생성
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject);

        Log.d("e",jsonObject);

        // POST 요청 보내기
        Call<PhotoResponse> sendCall = webService.sendDetectedObjects(requestBody);
        sendCall.enqueue(new Callback<PhotoResponse>() {
            @Override
            public void onResponse(Call<PhotoResponse> call, Response<PhotoResponse> response) {
                if (response.isSuccessful()) {
                    Log.d("Upload", "Detected objects sent successfully");
                    assert response.body() != null;
                    Log.d("Upload", "Common Photos: " + response.body().getPhotos().getCommonPhotos());
                    Log.d("Upload", "Individual Photos: " + response.body().getPhotos().getIndividualPhotos());
                    callbacks.onDetectedDataUploadSuccess(response.body());
                } else {
                    callbacks.onDetectedDataUploadFailure("Server responded with error");
                }
            }
            @Override
            public void onFailure(Call<PhotoResponse> call, Throwable t) {
                Log.e("Upload", "Error sending detected objects", t);
            }
        });
    }
}
