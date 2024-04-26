package com.example.metasearch.service;

import com.example.metasearch.model.CircleDetectionResponse;
import com.example.metasearch.model.PhotoResponse;
import com.example.metasearch.model.UploadResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Multipart;
import retrofit2.http.Part;

public interface ApiService {
    @Multipart
    @POST("upload")
    Call<CircleDetectionResponse> uploadImageAndCircles(
            @Part MultipartBody.Part image,
            @Part("source")RequestBody source,
            @Part("circles") RequestBody circles
    );
    @POST("android/circleToSearch")
    Call<PhotoResponse> sendDetectedObjects(@Body RequestBody detectedObjects);

    //AI서버에 추가 이미지 분석 요청
    @Multipart
    @POST("android/upload_add")
    Call<UploadResponse> uploadAddImage(@Part MultipartBody.Part image, @Part("source")RequestBody source);

    //AI서버에 삭제 이미지 요청
    @Multipart
    @POST("android/upload_delete")
    Call<UploadResponse> UploadDeleteImage(@Part MultipartBody.Part filename, @Part("source")RequestBody source);

    //AI서버에 데이터베이스 이미지 전송 요청
    @Multipart
    @POST("android/upload_database")
    Call<Void> uploadDatabaseImage(@Part MultipartBody.Part filename, @Part("source")RequestBody source);

    //AI서버에 마지막 전송 알림 요청
    @Multipart
    @POST("android/upload_finish")
    Call<Void> upload_finish(@Part("finish")RequestBody finish,@Part("source")RequestBody source);

    //Web서버에 이미지 전송 요청
    @Multipart
    @POST("android/uploadimg")
    Call<Void> uploadWebAddImage(@Part MultipartBody.Part image, @Part("source")RequestBody source);
}
