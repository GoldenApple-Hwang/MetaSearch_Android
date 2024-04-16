package com.example.metasearch.helper;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HttpHelper {
    private OkHttpClient okHttpClient;
    private Retrofit retrofit;
    private static HttpHelper httpHelperObject;

    private HttpHelper(String BASE_URL){
        okHttpClient = new OkHttpClient.Builder() //응답을 1분으로 지정
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient) // 위에서 설정한 OkHttpClient 인스턴스 사용
                .build();

    }
    //retrofit 반환
    public Retrofit getRetrofit(){
        return retrofit;
    }

    //HttpHelper 객체 반환
    public static HttpHelper getInstance(String BASE_URL){ //객체 생성(싱글톤 구현)
        if(httpHelperObject == null){
            httpHelperObject = new HttpHelper(BASE_URL);
        }
        return httpHelperObject;
    }
}
