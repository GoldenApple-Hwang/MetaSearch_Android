package com.example.metasearch.service.gptChat;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface OpenAIService {
    @Headers("Content-Type: application/json")
    @POST("/v1/chat/completions")
    Call<OpenAIResponse> createChatCompletion(
            @Header("Authorization") String authToken, @Body OpenAIRequest body);
}