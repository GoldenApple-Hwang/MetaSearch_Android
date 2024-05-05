package com.example.metasearch.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.example.metasearch.databinding.ActivityCircleToSearchBinding;
import com.example.metasearch.manager.AIRequestManager;
import com.example.metasearch.manager.GalleryImageManager;
import com.example.metasearch.manager.WebRequestManager;
import com.example.metasearch.model.Circle;
import com.example.metasearch.model.response.PhotoResponse;
import com.example.metasearch.ui.adapter.ImageAdapter;
import com.example.metasearch.ui.viewmodel.ImageViewModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CircleToSearchActivity extends AppCompatActivity
        implements ImageAdapter.OnImageClickListener,
        AIRequestManager.CircleDataUploadCallbacks,
        WebRequestManager.WebServerUploadCallbacks {
    private ActivityCircleToSearchBinding binding;
    private Uri imageUri;
    private ImageViewModel imageViewModel;
    private AIRequestManager aiRequestManager;
    private WebRequestManager webRequestManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();
        setupListeners();
        aiRequestManager = AIRequestManager.getAiImageUploader();
        webRequestManager = WebRequestManager.getWebImageUploader();
    }
    private void setupUI() {
        binding = ActivityCircleToSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
//        binding.customImageView.setImageUri(imageUri);
        // Glide를 사용하여 이미지 로드 및 표시(이미지 자동 회전 방지)
        Glide.with(this)
                .load(imageUri)
                .into(binding.customImageView);

        // circle to search 결과 검색된 사진 출력하는 세로 방향 RecyclerView 세팅
        setupRecyclerView();
    }
    private void setupRecyclerView() {
        ImageAdapter adapter = new ImageAdapter(new ArrayList<>(), this, this);
        binding.circleToSearchRecyclerView.setLayoutManager(new GridLayoutManager(this, 5));
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
            Toast.makeText(this, "관련 사진을 찾지 못했습니다.", Toast.LENGTH_SHORT).show();
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
    public void onCircleUploadSuccess(List<String> detectedObjects) {
        runOnUiThread(() -> {
            binding.btnSend.setEnabled(true);
            binding.spinKit.setVisibility(View.GONE);
            if (detectedObjects.isEmpty()) {
                Toast.makeText(this, "No objects detected.", Toast.LENGTH_LONG).show();
            } else {
                // Web Server로 이미지 분석 결과 전송
                webRequestManager.sendDetectedObjectsToAnotherServer(detectedObjects, "youjeong", this);
            }
        });
    }
    @Override
    public void onCircleUploadFailure(String message) {
        runOnUiThread(() -> {
            binding.btnSend.setEnabled(true);
            binding.spinKit.setVisibility(View.GONE);
            Toast.makeText(this, "Upload failed: " + message, Toast.LENGTH_LONG).show();
        });
    }
    @Override
    public void onWebServerUploadSuccess(PhotoResponse photoResponse) {
        binding.btnSend.setEnabled(true); // 버튼 활성화
        binding.spinKit.setVisibility(View.GONE); // 로딩 애니메이션 숨김
        updateRecyclerViewWithResponse(photoResponse);
    }
    @Override
    public void onWebServerUploadFailure(String message) {
        binding.btnSend.setEnabled(true); // 버튼 활성화
    }
}

