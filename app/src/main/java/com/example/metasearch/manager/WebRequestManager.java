package com.example.metasearch.manager;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.util.Log;

import com.example.metasearch.helper.HttpHelper;
import com.example.metasearch.model.request.ChangeNameRequest;
import com.example.metasearch.model.response.ChangeNameResponse;
import com.example.metasearch.model.response.PhotoNameResponse;
import com.example.metasearch.model.response.PhotoResponse;
import com.example.metasearch.service.ApiService;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

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
    public void uploadAddGalleryImage(ApiService service, ArrayList<String>addImagePaths, String dbName){
        Log.d(TAG,"web의 uploadAddGalleryImage 안에 들어옴");

        for(String addImagePath : addImagePaths){
            //Log.d(TAG,"addImge 이름 : " + imageFile.getName());
            File imageFile = new File(addImagePath);

            // 파일 이름을 URL 인코딩
            String fileName = null;
            try {
                fileName = URLEncoder.encode(imageFile.getName(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "파일 이름 인코딩 실패: " + e.getMessage());
                continue;
            }


            RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), imageFile);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", fileName, requestBody);

            Call<Void> call = service.uploadWebAddImage(imagePart, dbName); // sourceBody 대신 dbName을 직접 전달합니다.
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

    }



    // UI 업데이트를 위한 콜백 메서드
    public interface WebServerUploadCallbacks {
        void onWebServerUploadSuccess(PhotoResponse detectedObjects);
        void onWebServerUploadFailure(String message);
    }
    public interface WebServerPersonDataUploadCallbacks {
        void onPersonDataUploadSuccess(List<String> photoNameResponse);
        void onPersonDataUploadFailure(String message);
    }
    // Web Server로 인물 이름 or 사진 이름 전송
    public void sendPersonData(String personData, String dbName, WebServerPersonDataUploadCallbacks callbacks) {
        // Gson 인스턴스 생성
        Gson gson = new Gson();

        // dbName과 인물 정보(인물 이름 또는 사진 이름)을 포함하는 Map 객체 생성
        Map<String, String> jsonMap = new HashMap<>();
        jsonMap.put("dbName", dbName);
        jsonMap.put("personName", personData);

        // Map 객체를 JSON 문자열로 변환
        String jsonObject = gson.toJson(jsonMap);

        // JSON 문자열을 바디로 사용하여 RequestBody 객체 생성
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject);

        // POST 요청 보내기
        Call<List<String>> sendCall = webService.sendPersonData(requestBody);
        sendCall.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful()) {
                    Log.d("Upload", "Person data sent successfully");
                    assert response.body() != null;
                    Log.d("Upload", "Common Photos: " + response.body());
                    callbacks.onPersonDataUploadSuccess(response.body());
                } else {
                    callbacks.onPersonDataUploadFailure("Server responded with error");
                }
            }
            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                Log.e("Upload", "Error sending person data", t);
                callbacks.onPersonDataUploadFailure(t.getMessage());
            }
        });
    }
    // Web Server로 Circle to Search 이미지 분석 결과 전송
    public void sendDetectedObjectsToAnotherServer(List<String> detectedObjects, String dbName, WebRequestManager.WebServerUploadCallbacks callbacks) {
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
                    callbacks.onWebServerUploadSuccess(response.body());
                } else {
                    callbacks.onWebServerUploadFailure("Server responded with error");
                }
            }
            @Override
            public void onFailure(Call<PhotoResponse> call, Throwable t) {
                Log.e("Upload", "Error sending detected objects", t);
            }
        });
    }

    public void uploadDeleteGalleryImage(ApiService service, ArrayList<String> deleteImagePahts, String dbName ){
        RequestBody requestBody;
        MultipartBody.Part imagePart;
        for(String deleteImagePath : deleteImagePahts){
            requestBody = RequestBody.create(MediaType.parse("filename"),deleteImagePath);
            //파일의 경로를 해당 이미지의 이름으로 설정함
            imagePart = MultipartBody.Part.createFormData("deleteImage",deleteImagePath,requestBody);

            //API 호출
            Call<Void> call = service.uploadWebDeleteImage(imagePart,dbName); //이미지 업로드 API 호출
            call.enqueue(new Callback<Void>() { //비동기
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.e(TAG, "delete Image upload 성공: " + response.message());

                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.e(TAG, "삭제 이미지 업로드 실패함" + t.getMessage());
                }
            });
        }
    }

    public void changePersonName(String dbName, String oldName, String newName) {
        ChangeNameRequest request = new ChangeNameRequest(dbName, oldName, newName);
        webService.changeName(request).enqueue(new Callback<ChangeNameResponse>() {
            @Override
            public void onResponse(Call<ChangeNameResponse> call, Response<ChangeNameResponse> response) {
                if (response.isSuccessful()) {
                    // 성공적으로 이름 변경
                    Log.d("Name Change", "Success: " + response.body().getMessage());
                } else {
                    // 서버에서 정상적으로 처리하지 못했을 때
                    try {
                        Log.e("Name Change", "Failed: " + response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onFailure(Call<ChangeNameResponse> call, Throwable t) {
                // 네트워크 문제 등으로 요청 자체가 실패
                Log.e("Name Change", "Error: " + t.getMessage());
            }
        });
    }
}
