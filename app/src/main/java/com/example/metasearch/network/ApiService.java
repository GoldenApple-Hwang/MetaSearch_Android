package com.example.metasearch.network;

import com.example.metasearch.model.CircleDetectionResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
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
}
