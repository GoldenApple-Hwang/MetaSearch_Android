package com.example.metasearch.helper;

import com.example.metasearch.service.ApiService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
/**
 * API 서비스 인스턴스를 생성하기 위한 헬퍼 클래스
 * 여러 베이스 URL에 대해 각각의 Retrofit 인스턴스를 관리하기 위해 싱글톤 패턴 적용
 */
public class HttpHelper {
    // 각 baseUrl에 대응하는 HttpHelper 인스턴스를 저장하기 위한 Map
    private static Map<String, HttpHelper> instances = new HashMap<>();
    private Retrofit retrofit;
    /**
     * 생성자를 private으로 선언하여 외부에서 직접 인스턴스를 생성하지 못하게 함
     * @param baseUrl API 요청을 보낼 기본 URL
     */
    private HttpHelper(String baseUrl) {
        // OkHttpClient 설정: 연결, 읽기, 쓰기 타임아웃 설정
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(6000, TimeUnit.SECONDS)
                .readTimeout(6000, TimeUnit.SECONDS)
                .writeTimeout(6000, TimeUnit.SECONDS)
                .build();
        // Retrofit 인스턴스 생성
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }
    /**
     * baseUrl에 해당하는 HttpHelper 인스턴스 반환
     * 인스턴스가 없을 경우 새로 생성하여 반환
     * @param baseUrl API 요청을 보낼 기본 URL
     * @return HttpHelper 인스턴스
     */
    public static synchronized HttpHelper getInstance(String baseUrl) {
        if (!instances.containsKey(baseUrl)) {
            instances.put(baseUrl, new HttpHelper(baseUrl));
        }
        return instances.get(baseUrl);
    }
    /**
     * Retrofit 인스턴스 반환
     * @return Retrofit 인스턴스
     */
    public Retrofit getRetrofit() {
        return retrofit;
    }
    /**
     * 주어진 API 서비스 인터페이스에 대한 인스턴스 생성
     * @param apiServiceClass API 서비스 인터페이스의 클래스 객체
     * @return API 서비스 인터페이스의 인스턴스
     */
    public ApiService create(Class<ApiService> apiServiceClass) {
        return retrofit.create(apiServiceClass);
    }
}
