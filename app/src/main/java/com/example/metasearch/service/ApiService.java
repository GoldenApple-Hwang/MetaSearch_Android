package com.example.metasearch.service;

import com.example.metasearch.model.request.ChangeNameRequest;
import com.example.metasearch.model.request.DeleteEntityRequest;
import com.example.metasearch.model.request.PersonFrequencyRequest;
import com.example.metasearch.model.response.ChangeNameResponse;
import com.example.metasearch.model.response.CircleDetectionResponse;
import com.example.metasearch.model.response.DeleteEntityResponse;
import com.example.metasearch.model.response.PersonFrequencyResponse;
import com.example.metasearch.model.response.PhotoResponse;
import com.example.metasearch.model.response.PhotoNameResponse;
import com.example.metasearch.model.response.TripleResponse;
import com.example.metasearch.model.response.UploadResponse;
import com.example.metasearch.model.request.OpenAIRequest;
import com.example.metasearch.model.response.OpenAIResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

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
    // Web 서버에 인물 데이터(인물 이름) 수정 요청
    @POST("changename")
    Call<ChangeNameResponse> changeName(@Body ChangeNameRequest request);
    // Web 서버에 인물 빈도수 요청
    @POST("/api/peoplefrequency")
    Call<PersonFrequencyResponse> getPersonFrequency(@Body PersonFrequencyRequest request);
    // Web 서버에 사진 속성(이미지 설명 출력에 필요) 요청
    @GET("/api/photoTripleData/{dbName}/{photoName}")
    Call<TripleResponse> fetchTripleData(@Path("dbName") String dbName, @Path("photoName") String photoName);
    // Web 서버에 노드 삭제 요청
    @POST("neo4j/deleteEntity/")
    Call<DeleteEntityResponse> deleteEntity(@Body DeleteEntityRequest request);


    //AI서버에 추가 이미지 분석 요청
    @Multipart
    @POST("android/upload_add")
    Call<Void> uploadAddImage(@Part MultipartBody.Part image, @Part("dbName")RequestBody dbName);

    //AI서버에 삭제 이미지 요청
    @Multipart
    @POST("android/upload_delete")
    Call<UploadResponse> UploadDeleteImage(@Part MultipartBody.Part filename, @Part("dbName")RequestBody dbName);

    //AI서버에 데이터베이스 이미지 전송 요청
    @Multipart
    @POST("android/upload_database")
    Call<Void> uploadDatabaseImage(@Part MultipartBody.Part filename, @Part("dbName")RequestBody dbName);

    @Multipart
    @POST("android/upload_first")
    Call<Void> upload_first(@Part("first")RequestBody first,@Part("dbName")RequestBody dbName);

    //AI서버에 마지막 전송 알림 요청
    @Multipart
    @POST("android/upload_finish")
    Call<UploadResponse> upload_finish(@Part("finish")RequestBody finish,@Part("dbName")RequestBody dbName,@Part("rowCount")RequestBody rowCount);

    @Multipart
    @POST("android/upload_person_name")
    Call<Void> upload_person_name(@Part("dbName")RequestBody dbName,@Part("oldName")RequestBody oldName,@Part("newName")RequestBody newName);

    //Web서버에 이미지 전송 요청
    @Multipart
    @POST("android/uploadimg")
    Call<Void> uploadWebAddImage(@Part MultipartBody.Part image, @Query("dbName") String dbName);

    @Multipart
    @POST("android/deleteimg")
    Call<Void> uploadWebDeleteImage(@Part MultipartBody.Part filename, @Query("dbName") String dbName);




}
