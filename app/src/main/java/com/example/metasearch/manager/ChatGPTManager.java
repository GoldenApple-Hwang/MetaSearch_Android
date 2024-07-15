package com.example.metasearch.manager;

import com.example.metasearch.BuildConfig;
import com.example.metasearch.utils.HttpHelper;
import com.example.metasearch.data.model.Message;
import com.example.metasearch.network.request.OpenAIRequest;
import com.example.metasearch.network.response.OpenAIResponse;
import com.example.metasearch.network.api.ApiService;

import java.util.List;

import retrofit2.Callback;

public class ChatGPTManager {
    private static ChatGPTManager instance;
    private static final String BASE_URL = "https://api.openai.com/v1/";
    private static final String API_KEY = BuildConfig.OPENAI_API_KEY; // API 키 설정
    private ApiService apiService;

    // 생성자를 private으로 설정하여 외부에서 인스턴스 생성을 막음
    private ChatGPTManager() {
        this.apiService = HttpHelper.getInstance(BASE_URL).create(ApiService.class);
    }

    // 싱글톤 인스턴스를 반환하는 메서드
    public static synchronized ChatGPTManager getInstance() {
        if (instance == null) {
            instance = new ChatGPTManager();
        }
        return instance;
    }

    public void getChatResponse(List<Message> prompt, int maxTokens, Callback<OpenAIResponse> callback) {
        String authToken = "Bearer " + API_KEY;
        OpenAIRequest chatRequest = new OpenAIRequest("gpt-3.5-turbo", prompt);
        apiService.createChatCompletion(authToken, chatRequest).enqueue(callback);
    }


}
