package com.example.metasearch.ui.activity;

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
import com.example.metasearch.service.ApiService;
import com.example.metasearch.ui.CustomImageView;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ImageDisplayActivity extends AppCompatActivity {
    private CustomImageView customImageView;
    private Uri imageUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);

        customImageView = findViewById(R.id.customImageView);
        String imgUri = getIntent().getStringExtra("imageUri");
        customImageView.setImageUri(Uri.parse(imgUri));

        imageUri = Uri.parse(imgUri);

        Button btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener(v -> {
            try {
                sendCirclesAndImage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    private void sendCirclesAndImage() throws IOException {
        List<Circle> circles = customImageView.getCircles();
        if (imageUri != null && !circles.isEmpty()) {
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
                        Log.d("Upload", "Message: " + uploadResponse.getMessage());
                        List<String> detectedObjects = uploadResponse.getDetectedObjects();  // null-safe method 사용
                        // 다른 서버로 다시 보냄
                        sendDetectedObjectsToAnotherServer(uploadResponse.getDetectedObjects());
                        Log.d("Upload", "Detected Object: " + detectedObjects);
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
    private void sendDetectedObjectsToAnotherServer(List<String> detectedObjects) {
        Gson gson = new Gson();
        String jsonObject = gson.toJson(detectedObjects);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject);
        ApiService service = HttpHelper.getInstance("http://113.198.85.4/android/circleToSearch/youjeong").create(ApiService.class);
        Call<ResponseBody> sendCall = service.sendDetectedObjects(requestBody);
        Log.d("Upload", "Sending JSON: " + jsonObject);  // JSON 데이터 로깅
        sendCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("Upload", "Detected objects sent successfully");
                } else {
                    try {
                        Log.e("Upload", "Failed to send detected objects: " + response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Upload", "Error sending detected objects", t);
            }
        });
    }
}
