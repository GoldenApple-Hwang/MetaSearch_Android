package com.example.metasearch.manager;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.helper.HttpHelper;
import com.example.metasearch.model.Circle;
import com.example.metasearch.model.response.CircleDetectionResponse;
import com.example.metasearch.model.response.PhotoNameResponse;
import com.example.metasearch.model.response.UploadResponse;
import com.example.metasearch.service.ApiService;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class AIRequestManager {
    private static final String AIserver_BASE_URL = "http://113.198.85.5"; // ai 서버의 기본 url
    private static AIRequestManager aiImageUploader;
    private Retrofit aiRetrofit;
    private ApiService aiService;


    private AIRequestManager(){
        //this.aiService = AIHttpService.getInstance(AIserver_BASE_URL);
        this.aiService = HttpHelper.getInstance(AIserver_BASE_URL).getRetrofit().create(ApiService.class);
    }

    public ApiService getAiService(){
        return aiService;
    }

    public static AIRequestManager getAiImageUploader(){
        Log.d(TAG,"AIImageUploaderController 함수 들어옴 객체 생성 or 반환");
        if(aiImageUploader == null){
            aiImageUploader = new AIRequestManager();

        }
        return aiImageUploader;
    }

    public void completeUploadImage(DatabaseHelper databaseHelper,ApiService service,String DBName){
        RequestBody requestBody;
        MultipartBody.Part imagePart;

        //이미지 출처 정보를 전송할 RequestBody 생성
        RequestBody finishBody = RequestBody.create(MediaType.parse("text/plain"),"finish"); //DB이름과 어디에 저장되어야하는지에 관한 정보를 전달
        RequestBody sourceBody = RequestBody.create(MediaType.parse("text/plain"),DBName); //DB이름과 어디에 저장되어야하는지에 관한 정보를 전달
        Call<UploadResponse> call = service.upload_finish(finishBody,sourceBody); //이미지 업로드 API 호출
        call.enqueue(new Callback<UploadResponse>() { //비동기
            @Override
            public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                if (response.isSuccessful()) {
                    Log.e(TAG, "Image upload 성공: " + response.message());
                    if(response.body() != null) {
                        for (UploadResponse.ImageData imageData : response.body().getImages()) {
                            boolean isFaceExit = imageData.getIsExit(); //추출된 이미지가 있다는 것
                            if (isFaceExit) {
                                //Log.d(TAG,"추출한 얼굴 이미지 있음");
                                Log.d(TAG, "추가 이미지에 대한 응답 받음 / 추가할 얼굴 있음");
                                String imageName = imageData.getImageName();
                                String imageBytes = imageData.getImageBytes();
                                // 여기서 바이트 배열로 변환 후 이미지 처리를 할 수 있음
                                byte[] decodedString = Base64.decode(imageBytes, Base64.DEFAULT);
                                Log.d(TAG, "imageName : " + imageName);
                                databaseHelper.insertImage(imageName, decodedString);
                            } else {
                                Log.d(TAG, "추가 이미지에 대한 응답 받음 / 추가할 얼굴 없음");
                                //Log.e(TAG, "이미지 업로드 성공, 추출된 이미지 없음" + response.message());
                            }
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<UploadResponse> call, Throwable t) {
                Log.e(TAG, "추가 이미지 업로드 실패함" + t.getMessage());
            }
        });
    }
    //추가된 이미지 관해 서버에 전송
    public CompletableFuture<Void> uploadAddGalleryImage( ArrayList<String> imagePaths, ApiService service, String source) throws IOException {
        List<CompletableFuture<Void>> futuresList = new ArrayList<>();
        Log.d(TAG,"uploadAddGalleryImage 안에 들어옴");
        //List<CompletableFuture<Void>> futuresList = new ArrayList<>();
        //Log.d(TAG,"addImge 이름 : " + imageFile.getName());
        RequestBody requestBody;
        MultipartBody.Part imagePart;
        for(String addImagePath : imagePaths){
            CompletableFuture<Void> future = new CompletableFuture<>();
            futuresList.add(future);

            File imageFile = new File(addImagePath);

            //추가 이미지 분석이 필요한 이미지를 전달
            requestBody = RequestBody.create(MediaType.parse("image"),imageFile);
            //파일의 경로를 해당 이미지의 이름으로 설정함
            imagePart = MultipartBody.Part.createFormData("addImage",imageFile.getName(),requestBody);


            //이미지 출처 정보를 전송할 RequestBody 생성
            RequestBody sourceBody = RequestBody.create(MediaType.parse("text/plain"),source); //DB이름과 어디에 저장되어야하는지에 관한 정보를 전달
            //API 호출
            Call<Void> call = service.uploadAddImage(imagePart,sourceBody); //이미지 업로드 API 호출
            call.enqueue(new Callback<Void>() { //비동기
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.e(TAG, "Image upload 성공: " + response.message());
                        // 서버로부터 받은 응답 처리
//                        if(response.body() != null) {
//                            for (UploadResponse.ImageData imageData : response.body().getImages()) {
//                                boolean isFaceExit = imageData.getIsExit(); //추출된 이미지가 있다는 것
//                                if (isFaceExit) {
//                                    //Log.d(TAG,"추출한 얼굴 이미지 있음");
//                                    Log.d(TAG, "추가 이미지에 대한 응답 받음 / 추가할 얼굴 있음");
//                                    String imageName = imageData.getImageName();
//                                    String imageBytes = imageData.getImageBytes();
//                                    // 여기서 바이트 배열로 변환 후 이미지 처리를 할 수 있음
//                                    byte[] decodedString = Base64.decode(imageBytes, Base64.DEFAULT);
//                                    Log.d(TAG, "imageName : " + imageName);
//                                    databaseHelper.insertImage(imageName, decodedString);
//                                } else {
//                                    Log.d(TAG, "추가 이미지에 대한 응답 받음 / 추가할 얼굴 없음");
//                                    //Log.e(TAG, "이미지 업로드 성공, 추출된 이미지 없음" + response.message());
//                                }
//                            }
//                        }
                        future.complete(null);
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.e(TAG, "추가 이미지 업로드 실패함" + t.getMessage());
                    //future.completeExceptionally(t); // 작업 실패 시 future에 예외를 설정
                    future.complete(null);
                }
            });
        }
        return CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[0]));
    }

    //삭제된 이미지 관해 서버에 전송
    public CompletableFuture<Void> uploadDeleteGalleryImage(DatabaseHelper databaseHelper, ArrayList<String>deleteImagePaths, ApiService service, String source){
        List<CompletableFuture<Void>> futuresList = new ArrayList<>();

        RequestBody requestBody;
        MultipartBody.Part imagePart;

        for(String deleteImagePath : deleteImagePaths){
            CompletableFuture<Void> future = new CompletableFuture<>();
            futuresList.add(future);
            requestBody = RequestBody.create(MediaType.parse("filename"),deleteImagePath);
            //파일의 경로를 해당 이미지의 이름으로 설정함
            imagePart = MultipartBody.Part.createFormData("deleteImage",deleteImagePath,requestBody);

            //이미지 출처 정보를 전송할 RequestBody 생성
            RequestBody sourceBody = RequestBody.create(MediaType.parse("text/plain"),source); //DB이름과 어디에 저장되어야하는지에 관한 정보를 전달
            // RequestBody endIndicatorBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(isFinal)); //갤러리의 마지막 요청인지에 대한 정보 전달

            //API 호출
            Call<UploadResponse> call = service.UploadDeleteImage(imagePart,sourceBody); //이미지 업로드 API 호출
            call.enqueue(new Callback<UploadResponse>() { //비동기
                @Override
                public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                    if (response.isSuccessful()) {
                        //Log.e(TAG, "Image upload 성공: " + response.message());
                        // 서버로부터 받은 응답 처리
                        if(response.body() != null){
                            for (UploadResponse.ImageData imageData : response.body().getImages()) {
                                boolean isExit = imageData.getIsExit();
                                if(isExit){
                                    Log.d(TAG,"삭제 이미지에 대한 응답 받음 / 삭제할 얼굴 있음");
                                    String imageName = imageData.getImageName();
//                                String imageBytes = imageData.getImageBytes();
//                                // 여기서 바이트 배열로 변환 후 이미지 처리를 할 수 있음
//                                byte[] decodedString = Base64.decode(imageBytes, Base64.DEFAULT);
                                    Log.d(TAG,"imageName : "+imageName);
                                    databaseHelper.deleteImage(imageName); //이미지 이름을 통해 해당 데이터 삭제함
                                }
                                else{
                                    Log.e(TAG, "삭제 이미지에 대한 응답 받음 / 삭제할 얼굴 없음" + response.message());
                                }
                            }
                        }
                    }
                }
                @Override
                public void onFailure(Call<UploadResponse> call, Throwable t) {
                    Log.e(TAG, "삭제 이미지 업로드 실패함" + t.getMessage());
                }
            });
        }


        return CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[0]));

    }

    //Database 이미지 서버에 전송
    public CompletableFuture<Void> uploadDBImage(ApiService service, Map<String, byte[]> imagesList, String source){
        //데이터베이스 서버 요청이 다 끝나면, 다 끝났다는 것을 반환함
        Log.d(TAG,"uploadDBImage 들어옴");
        // 모든 비동기 작업을 추적하기 위한 CompletableFuture 리스트 생성
        List<CompletableFuture<Void>> futuresList = new ArrayList<>();
        for (Map.Entry<String,byte[]> database_image_element : imagesList.entrySet()){
            CompletableFuture<Void> future = new CompletableFuture<>();
            futuresList.add(future);
            //byte[]에서 RequestBody 생성
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"),database_image_element.getValue());
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("faceImage",database_image_element.getKey(),requestBody);

            //이미지 출처 정보를 전송할 RequestBody 생성
            RequestBody sourceBody = RequestBody.create(MediaType.parse("text/plain"),source);

            //API 호출
            Call<Void> call = service.uploadDatabaseImage(imagePart,sourceBody); //이미지 업로드 API 호출
            call.enqueue(new Callback<Void>() { //비동기
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.e(TAG, "데이터베이스 업로드 성공함" + response.message());
                        future.complete(null); // 작업 성공 시 future를 완료 상태로 설정
                    }

                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.e(TAG, "데이터베이스 업로드 실패함" + t.getMessage());
                    future.completeExceptionally(t); // 작업 실패 시 future에 예외를 설정
                }
            });
        }
        // 모든 futures가 완료될 때까지 기다림
        return CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[0]));
    }



    // UI 업데이트를 위한 콜백 메서드
    public interface CircleDataUploadCallbacks {
        void onCircleUploadSuccess(List<String> detectedObjects);
        void onCircleUploadFailure(String message);
    }
    // AI Server로 이미지와 원 리스트 전송
    public void uploadCircleData(Uri imageUri, List<Circle> circles, String source, Context context, CircleDataUploadCallbacks callbacks) throws IOException {
        File file = UriToFileConverter.getFileFromUri(context, imageUri);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("searchImage", file.getName(), requestFile);

        Gson gson = new Gson();
        String jsonCircles = gson.toJson(circles);
        RequestBody circleData = RequestBody.create(MediaType.parse("application/json"), jsonCircles);
        RequestBody sourceBody = RequestBody.create(MediaType.parse("text/plain"), source);

        Call<CircleDetectionResponse> call = aiService.uploadImageAndCircles(body, sourceBody, circleData);
        call.enqueue(new Callback<CircleDetectionResponse>() {
            @Override
            public void onResponse(Call<CircleDetectionResponse> call, Response<CircleDetectionResponse> response) {
                if (response.isSuccessful()) {
                    callbacks.onCircleUploadSuccess(response.body().getDetectedObjects());
                } else {
                    callbacks.onCircleUploadFailure("Server responded with error");
                }
            }

            @Override
            public void onFailure(Call<CircleDetectionResponse> call, Throwable t) {
                callbacks.onCircleUploadFailure("Failed to upload data and image: " + t.getMessage());
            }
        });
    }
}
