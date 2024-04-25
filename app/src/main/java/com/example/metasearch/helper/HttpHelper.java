package com.example.metasearch.helper;

import com.example.metasearch.service.ApiService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HttpHelper {
    private static Map<String, HttpHelper> instances = new HashMap<>();
    private Retrofit retrofit;

    private HttpHelper(String baseUrl) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }

    public static synchronized HttpHelper getInstance(String baseUrl) {
        if (!instances.containsKey(baseUrl)) {
            instances.put(baseUrl, new HttpHelper(baseUrl));
        }
        return instances.get(baseUrl);
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    public ApiService create(Class<ApiService> apiServiceClass) {
        return retrofit.create(apiServiceClass);
    }
}
