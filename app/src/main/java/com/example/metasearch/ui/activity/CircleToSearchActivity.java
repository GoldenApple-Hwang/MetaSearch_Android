package com.example.metasearch.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.example.metasearch.databinding.ActivityCircleToSearchBinding;
import com.example.metasearch.helper.HttpHelper;
import com.example.metasearch.manager.AIRequestManager;
import com.example.metasearch.manager.GalleryImageManager;
import com.example.metasearch.manager.UriToFileConverter;
import com.example.metasearch.model.Circle;
import com.example.metasearch.model.response.CircleDetectionResponse;
import com.example.metasearch.model.response.PhotoResponse;
import com.example.metasearch.service.ApiService;
import com.example.metasearch.ui.adapter.ImageAdapter;
import com.example.metasearch.ui.viewmodel.ImageViewModel;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CircleToSearchActivity extends AppCompatActivity implements ImageAdapter.OnImageClickListener, AIRequestManager.CircleDataUploadCallbacks {
    private static final String AI_SERVER_URL = "http://113.198.85.5";
    private static final String WEB_SERVER_URL = "http://113.198.85.4";
    private ActivityCircleToSearchBinding binding;
    private Uri imageUri;
    private ImageViewModel imageViewModel;
    private AIRequestManager aiRequestManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();
        setupListeners();
        aiRequestManager = AIRequestManager.getAiImageUploader();
    }
    private void setupUI() {
        binding = ActivityCircleToSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
//        binding.customImageView.setImageUri(imageUri);
        // Glide를 사용하여 이미지 로드 및 표시
        // 이미지 자동 회전 방지
        Glide.with(this)
                .load(imageUri)
                .into(binding.customImageView);

        // circle to search 결과 검색된 사진 출력하는 세로 방향 RecyclerView 세팅
        setupRecyclerView();
    }
    private void setupRecyclerView() {
        ImageAdapter adapter = new ImageAdapter(new ArrayList<>(), this, this);
        binding.circleToSearchRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        binding.circleToSearchRecyclerView.setAdapter(adapter);
    }
    private void setupListeners() {
        binding.btnSend.setOnClickListener(v -> {
            try {
                sendCirclesAndImage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        binding.btnReset.setOnClickListener(v -> binding.customImageView.clearCircles());
    }
    private void sendCirclesAndImage() throws IOException {
        List<Circle> circles = binding.customImageView.getCircles();
        if (imageUri != null && !circles.isEmpty()) {
            binding.btnSend.setEnabled(false); // 버튼 비활성화
            // 요청 전에 로딩 애니메이션 표시
            binding.spinKit.setVisibility(View.VISIBLE);

            // AI Server로 이미지와 원 리스트 전송
            aiRequestManager.uploadCircleData(imageUri, circles, "source", this, this);
        } else {
            Toast.makeText(this, "이미지 또는 원 정보가 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }
    // Web Server로 이미지 분석 결과 전송
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

        // HttpHelper를 사용하여 ApiService 인스턴스 생성
        ApiService service = HttpHelper.getInstance(WEB_SERVER_URL).getRetrofit().create(ApiService.class);

        // POST 요청 보내기
        Call<PhotoResponse> sendCall = service.sendDetectedObjects(requestBody);
        sendCall.enqueue(new Callback<PhotoResponse>() {
            @Override
            public void onResponse(Call<PhotoResponse> call, Response<PhotoResponse> response) {
                if (response.isSuccessful()) {
                    Log.d("Upload", "Detected objects sent successfully");
                    PhotoResponse photoResponse = response.body();

                    updateRecyclerViewWithResponse(photoResponse);

                    Log.d("Upload", "Common Photos: " + photoResponse.getPhotos().getCommonPhotos());
                    Log.d("Upload", "Individual Photos: " + photoResponse.getPhotos().getIndividualPhotos());
                } else {
                    try {
                        Log.e("Upload", "Failed to send detected objects: " + response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                binding.btnSend.setEnabled(true); // 버튼 활성화
                // 로딩 애니메이션 숨김
                binding.spinKit.setVisibility(View.GONE);
            }
            @Override
            public void onFailure(Call<PhotoResponse> call, Throwable t) {
                Log.e("Upload", "Error sending detected objects", t);
                binding.btnSend.setEnabled(true); // 버튼 활성화
            }
        });
    }
    private void updateRecyclerView(List<Uri> imageUris) {
        ImageAdapter adapter = new ImageAdapter(imageUris, this, this);
        binding.circleToSearchRecyclerView.setAdapter(adapter);
    }

    private void updateRecyclerViewWithResponse(PhotoResponse photoResponse) {
        List<Uri> matchedUris = new ArrayList<>();
        if (photoResponse != null && photoResponse.getPhotos() != null) {
            List<String> allPhotoNames = new ArrayList<>();

            // Common photos 처리
            allPhotoNames.addAll(photoResponse.getPhotos().getCommonPhotos());

            // Individual photos 처리
            for (List<String> names : photoResponse.getPhotos().getIndividualPhotos().values()) {
                allPhotoNames.addAll(names);
            }
            // 이미지 이름 리스트에서 확장자를 제거하지 않고 사용
            matchedUris = GalleryImageManager.findMatchedUris(allPhotoNames, this);
        }

        ImageAdapter adapter = new ImageAdapter(matchedUris, this, this);
        binding.circleToSearchRecyclerView.setAdapter(adapter);
        updateUIWithMatchedUris(matchedUris, photoResponse);
    }
    private void updateUIWithMatchedUris(List<Uri> matchedUris, PhotoResponse photoResponse) {
        if (imageViewModel == null) {
            imageViewModel = new ViewModelProvider(this).get(ImageViewModel.class);
        }
        imageViewModel.setImageUris(matchedUris);

        if (!matchedUris.isEmpty()) {
            ImageAdapter adapter = (ImageAdapter) binding.circleToSearchRecyclerView.getAdapter();
            if (adapter != null) {
                adapter.updateData(matchedUris);
            }
        } else {
            Toast.makeText(this, "No matched images found.", Toast.LENGTH_SHORT).show();
        }

        // 카테고리 이름만 TextView에 표시
        StringBuilder categories = new StringBuilder();
        photoResponse.getPhotos().getIndividualPhotos().keySet().forEach(category -> {
            categories.append(category).append("\n");
        });
        binding.textViewRelatedCategories.setText(categories.toString().trim());
    }
    @Override
    public void onImageClick(Uri uri) {
        Intent intent = new Intent(this, ImageDisplayActivity.class);
        intent.putExtra("imageUri", uri.toString());
        startActivity(intent);
    }
    @Override
    public void onUploadSuccess(List<String> detectedObjects) {
        runOnUiThread(() -> {
            binding.btnSend.setEnabled(true);
            binding.spinKit.setVisibility(View.GONE);
            if (detectedObjects.isEmpty()) {
                Toast.makeText(this, "No objects detected.", Toast.LENGTH_LONG).show();
            } else {
                sendDetectedObjectsToAnotherServer(detectedObjects, "youjeong");
            }
        });
    }

    @Override
    public void onUploadFailure(String message) {
        runOnUiThread(() -> {
            binding.btnSend.setEnabled(true);
            binding.spinKit.setVisibility(View.GONE);
            Toast.makeText(this, "Upload failed: " + message, Toast.LENGTH_LONG).show();
        });
    }
}

