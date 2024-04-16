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
import com.example.metasearch.network.ApiService;
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
import retrofit2.Retrofit;

public class ImageDisplayActivity extends AppCompatActivity {
    private CustomImageView customImageView;
    private Uri imageUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);

//        ImageView imageView = findViewById(R.id.imageViewFullScreen);
//        String imageUri = getIntent().getStringExtra("imageUri");
//        imageView.setImageURI(Uri.parse(imageUri));

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

        Retrofit retrofit = HttpHelper.getInstance("http://113.198.85.5").getRetrofit();
        ApiService service = retrofit.create(ApiService.class);
        Call<ResponseBody> call = service.uploadImageAndCircles(body, sourceBody, circleData);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        // 응답 본문을 문자열로 변환
                        String responseBodyString = response.body().string();
                        Log.d("Upload", "Response body: " + responseBodyString);
                    } catch (IOException e) {
                        Log.e("Upload", "Error reading response body", e);
                    }
                } else {
                    // 에러 응답 처리
                    Log.e("Upload", "Request failed with status: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e("Upload", "Error body: " + response.errorBody().string());
                        }
                    } catch (IOException e) {
                        Log.e("Upload", "Error reading error body", e);
                    }
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Upload", "Failed to upload data and image", t);
            }
        });
    }
}
