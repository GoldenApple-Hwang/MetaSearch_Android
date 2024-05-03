package com.example.metasearch.service;

import com.example.metasearch.model.response.CircleDetectionResponse;
import com.example.metasearch.model.response.PhotoResponse;
import com.example.metasearch.model.response.PhotoNameResponse;
import com.example.metasearch.model.response.UploadResponse;
import com.example.metasearch.model.request.OpenAIRequest;
import com.example.metasearch.model.response.OpenAIResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Multipart;
import retrofit2.http.Part;

public interface ApiService {
    // OpenAI service
    @Headers("Content-Type: application/json")
    @POST("/v1/chat/completions")
    Call<OpenAIResponse> createChatCompletion(
            @Header("Authorization") String authToken, @Body OpenAIRequest body);
    // AI 서버에 이미지 분석 요청(circle to search)
    @Multipart
    @POST("android/circle_search")
    Call<CircleDetectionResponse> uploadImageAndCircles(
            @Part MultipartBody.Part image,
            @Part("dbName")RequestBody dbName,
            @Part("circles") RequestBody circles
    );
    // Web 서버에 이미지 속성 전송 후 이미지 전송 요청(circle to search)
    @POST("android/circleToSearch")
    Call<PhotoResponse> sendDetectedObjects(@Body RequestBody detectedObjects);
    // Web 서버에 사이퍼 쿼리 전송 후 이미지 전송 요청(NL search)
    @POST("/nlqsearch")
    Call<PhotoNameResponse> sendCypherQuery(@Body RequestBody body);
    // Web 서버에 인물 이름 or 사진 이름 전송 후 이미지 전송 요청(people search)
    @POST("personsearch")
    Call<List<String>> sendPersonData(@Body RequestBody body);


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
