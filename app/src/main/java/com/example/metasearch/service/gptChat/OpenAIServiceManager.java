package com.example.metasearch.service.gptChat;

import com.example.metasearch.BuildConfig;
import com.example.metasearch.helper.HttpHelper;
//import com.example.metasearch.helper.HttpHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OpenAIServiceManager {
    private static final String BASE_URL = "https://api.openai.com/";
    private final Retrofit retrofit;

    public OpenAIServiceManager() {
        retrofit = HttpHelper.getInstance(BASE_URL).getRetrofit();
    }

    public void fetchOpenAIResponse(String userInput, Callback<OpenAIResponse> callback) {
        if (userInput.length() == 0) return;

        OpenAIService service = retrofit.create(OpenAIService.class);
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", userInput));
        OpenAIRequest request = new OpenAIRequest("gpt-3.5-turbo", messages);
        String apiKey = "Bearer " + BuildConfig.OPENAI_API_KEY;

        service.createChatCompletion(apiKey, request).enqueue(callback);
    }
}
