package com.example.metasearch.manager;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.util.Log;

import com.example.metasearch.helper.HttpHelper;
import com.example.metasearch.service.ApiService;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class WebImageUploadManager {
    private static final String Webserver_BASE_URL = "http://113.198.85.4"; // ai 서버의 기본 url
    private static WebImageUploadManager webImageUploader;
    private Retrofit webRetrofit;
    private ApiService webService;

    private WebImageUploadManager(){
        //this.aiService = AIHttpService.getInstance(AIserver_BASE_URL);
        this.webService = HttpHelper.getInstance(Webserver_BASE_URL).getRetrofit().create(ApiService.class);
    }

    public ApiService getWebService(){
        return webService;
    }

    public static WebImageUploadManager getWebImageUploader(){
        Log.d(TAG,"AIImageUploaderController 함수 들어옴 객체 생성 or 반환");
        if(webImageUploader == null){
            webImageUploader = new WebImageUploadManager();

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
}
