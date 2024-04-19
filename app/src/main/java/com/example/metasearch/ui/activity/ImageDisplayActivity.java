package com.example.metasearch.ui.activity;

import android.app.AutomaticZenRule;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.metasearch.R;
import com.example.metasearch.helper.HttpHelper;
import com.example.metasearch.manager.UriToFileConverter;
import com.example.metasearch.model.Circle;
import com.example.metasearch.model.CircleDetectionResponse;
import com.example.metasearch.model.PhotoResponse;
import com.example.metasearch.service.ApiService;
import com.example.metasearch.ui.CustomImageView;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ImageDisplayActivity extends AppCompatActivity {
    private CustomImageView customImageView;
    private Button btnSend;
    private Uri imageUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);

        customImageView = findViewById(R.id.customImageView);
        String imgUri = getIntent().getStringExtra("imageUri");
        customImageView.setImageUri(Uri.parse(imgUri));

        imageUri = Uri.parse(imgUri);

        btnSend = findViewById(R.id.btnSend); // 검색 버튼
        Button btnReset = findViewById(R.id.btnReset); // 그린 원 초기화 버튼
        btnSend.setOnClickListener(v -> {
            try {
                sendCirclesAndImage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        btnReset.setOnClickListener(v -> customImageView.clearCircles());
    }
    private void sendCirclesAndImage() throws IOException {
        List<Circle> circles = customImageView.getCircles();
        if (imageUri != null && !circles.isEmpty()) {
            btnSend.setEnabled(false); // 버튼 비활성화
            uploadData(imageUri, circles, "source");
        } else {
            Toast.makeText(this, "이미지 또는 원 정보가 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadData(Uri imageUri, List<Circle> circles, String source) throws IOException {
        // Uri를 파일로 변환
        File file = UriToFileConverter.getFileFromUri(this, imageUri);

        // 파일을 사용하여 나머지 업로드 로직을 계속 수행
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("searchImage", file.getName(), requestFile);

        Gson gson = new Gson();
        String jsonCircles = gson.toJson(circles);
        RequestBody circleData = RequestBody.create(MediaType.parse("application/json"), jsonCircles);

        //DB이름과 어디에 저장되어야하는지에 관한 정보를 전달
        RequestBody sourceBody = RequestBody.create(MediaType.parse("text/plain"),source);

        ApiService service = HttpHelper.getInstance("http://113.198.85.5").getRetrofit().create(ApiService.class);
        Call<CircleDetectionResponse> call = service.uploadImageAndCircles(body, sourceBody, circleData);
        call.enqueue(new Callback<CircleDetectionResponse>() {
            @Override
            public void onResponse(Call<CircleDetectionResponse> call, Response<CircleDetectionResponse> response) {
                if (response.isSuccessful()) {
                    CircleDetectionResponse uploadResponse = response.body();
                    if (uploadResponse != null) {
                        List<String> detectedObjects = uploadResponse.getDetectedObjects();  // null-safe method 사용
                        if (detectedObjects == null || detectedObjects.isEmpty()) {
                            // detectedObjects가 널이거나 비어있으면 토스트 메시지를 띄움
                            runOnUiThread(() -> Toast.makeText(ImageDisplayActivity.this, "탐지된 객체가 없습니다.", Toast.LENGTH_LONG).show());
                            btnSend.setEnabled(true); // 버튼 활성화
                        } else {
                            // detectedObjects가 널이 아니고 비어있지 않으면 다른 서버로 데이터 전송
                            sendDetectedObjectsToAnotherServer(detectedObjects, "youjeong");
                            runOnUiThread(() -> Toast.makeText(ImageDisplayActivity.this, detectedObjects.toString(), Toast.LENGTH_LONG).show());
                            Log.d("Upload", "Detected Object: " + detectedObjects);
                        }
                    } else {
                        Log.e("Upload", "Response body is null");
                    }
                } else {
                    Log.e("Upload", "Error: " + response.errorBody());
                }
            }
            @Override
            public void onFailure(Call<CircleDetectionResponse> call, Throwable t) {
                Log.e("Upload", "Failed to upload data and image", t);
            }

        });
    }
    private void sendDetectedObjectsToAnotherServer(List<String> detectedObjects, String dbName) {
        // Gson 인스턴스 생성
        Gson gson = new Gson();

        // detectedObjects와 dbName을 포함하는 Map 객체 생성
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("dbName", dbName);
        jsonMap.put("properties", detectedObjects);

        // Map 객체를 JSON 문자열로 변환
        String jsonObject = gson.toJson(jsonMap);

        // JSON 문자열을 바디로 사용하여 RequestBody 객체 생성
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject);

        Log.d("e",jsonObject);

        OkHttpClient okHttpClient = new OkHttpClient.Builder() //응답을 1분으로 지정
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://113.198.85.4")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient) // 위에서 설정한 OkHttpClient 인스턴스 사용
                .build();

        // HttpHelper를 사용하여 ApiService 인스턴스 생성
        ApiService service = retrofit.create(ApiService.class);

        // POST 요청 보내기
        Call<PhotoResponse> sendCall = service.sendDetectedObjects(requestBody);
        sendCall.enqueue(new Callback<PhotoResponse>() {
            @Override
            public void onResponse(Call<PhotoResponse> call, Response<PhotoResponse> response) {
                if (response.isSuccessful()) {
                    Log.d("Upload", "Detected objects sent successfully");
                    PhotoResponse photoResponse = response.body();
                    Log.d("Upload", "Common Photos: " + photoResponse.getPhotos().getCommonPhotos());
                    Log.d("Upload", "Individual Photos: " + photoResponse.getPhotos().getIndividualPhotos());
                } else {
                    try {
                        Log.e("Upload", "Failed to send detected objects: " + response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                btnSend.setEnabled(true); // 버튼 활성화
            }
            @Override
            public void onFailure(Call<PhotoResponse> call, Throwable t) {
                Log.e("Upload", "Error sending detected objects", t);
                btnSend.setEnabled(true); // 버튼 활성화
            }
        });
    }

}
