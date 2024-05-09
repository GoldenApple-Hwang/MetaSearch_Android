package com.example.metasearch.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.metasearch.R;
import com.example.metasearch.databinding.ActivityCircleToSearchBinding;
import com.example.metasearch.manager.AIRequestManager;
import com.example.metasearch.manager.GalleryImageManager;
import com.example.metasearch.manager.WebRequestManager;
import com.example.metasearch.model.Circle;
import com.example.metasearch.model.response.PhotoResponse;
import com.example.metasearch.ui.adapter.ImageAdapter;
import com.example.metasearch.ui.viewmodel.ImageViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.github.muddz.styleabletoast.StyleableToast;
import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog;
import me.jfenn.colorpickerdialog.interfaces.OnColorPickedListener;

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
        StyleableToast.makeText(this, "드래그 해서 원을 그려주세요.", R.style.customToast).show();
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

        // 화면 크기의 1/2 높이로 CustomImageView 크기 조정
        int halfScreenHeight = getResources().getDisplayMetrics().heightPixels / 2;
        ViewGroup.LayoutParams layoutParams = binding.customImageView.getLayoutParams();
        layoutParams.height = halfScreenHeight;
        binding.customImageView.setLayoutParams(layoutParams);

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
    private void showColorPickerDialog() {
        new ColorPickerDialog()
                .withColor(getResources().getColor(R.color.white)) // 기본 색상
                .withListener(new OnColorPickedListener<ColorPickerDialog>() {
                    @Override
                    public void onColorPicked(@Nullable ColorPickerDialog dialog, int color) {
                        // 선택한 색상 사용
                        binding.customImageView.setCircleColor(color);
                    }
                })
                .show(getSupportFragmentManager(), "colorPicker");
    }
    private void setupListeners() {
        binding.circleMenu.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.search) {
                    try {
                        sendCirclesAndImage();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                } else if (itemId == R.id.reset) {
                    // 리셋 버튼 클릭 시 모든 원 삭제
                    binding.customImageView.clearCircles();
                    return true;
                } else if (itemId == R.id.color) {
                    // 컬러 버튼 클릭 시 컬러 피커 다이얼로그 표시
                    showColorPickerDialog();
                    return true;
                }
                return false;
            }
        });
        binding.circleToSearchRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    // 스크롤 내릴 때, 네비게이션 바 숨기기
                    hideBottomNavigationView();
                } else if (dy < 0) {
                    // 스크롤 올릴 때, 네비게이션 바 보이기
                    showBottomNavigationView();
                }
            }
        });
    }
    // 하단의 네비 바 숨김
    public void hideBottomNavigationView() {
        binding.circleMenu.animate().translationY(binding.circleMenu.getHeight());
    }
    // 하단의 네비 바 보임
    public void showBottomNavigationView() {
        binding.customImageView.animate().translationY(0);
    }

    private void sendCirclesAndImage() throws IOException {
        List<Circle> circles = binding.customImageView.getCircles();
        if (imageUri != null && !circles.isEmpty()) {
//            binding.circleMenu.setEnabled(false); // 버튼 비활성화
            // 메뉴 아이템을 직접 비활성화
            MenuItem searchItem = binding.circleMenu.getMenu().findItem(R.id.search);
            searchItem.setEnabled(false);
            // 요청 전에 로딩 애니메이션 표시
            binding.spinKit.setVisibility(View.VISIBLE);

            // AI Server로 이미지와 원 리스트 전송
            aiRequestManager.uploadCircleData(imageUri, circles, "source", this, this);
        } else {
//            Toast.makeText(this, "이미지 또는 원 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            StyleableToast.makeText(this, "이미지 또는 원 정보가 없습니다. 드래그 해서 원을 그려주세요.", R.style.customToast).show();
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
            StyleableToast.makeText(this, "관련 사진을 찾지 못했습니다.", R.style.customToast).show();
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
//            binding.circleMenu.setEnabled(true);
            MenuItem searchItem = binding.circleMenu.getMenu().findItem(R.id.search);
            searchItem.setEnabled(true);
            binding.spinKit.setVisibility(View.GONE);
            if (detectedObjects.isEmpty()) {
                StyleableToast.makeText(this, "No objects detected.", R.style.customToast).show();
            } else {
                // Web Server로 이미지 분석 결과 전송
                webRequestManager.sendDetectedObjectsToAnotherServer(detectedObjects, "youjeong", this);
            }
        });
    }
    @Override
    public void onCircleUploadFailure(String message) {
        runOnUiThread(() -> {
//            binding.circleMenu.setEnabled(true);
            MenuItem searchItem = binding.circleMenu.getMenu().findItem(R.id.search);
            searchItem.setEnabled(true);
            binding.spinKit.setVisibility(View.GONE);
            StyleableToast.makeText(this, "Upload failed: " + message, R.style.customToast).show();
        });
    }
    @Override
    public void onWebServerUploadSuccess(PhotoResponse photoResponse) {
        MenuItem searchItem = binding.circleMenu.getMenu().findItem(R.id.search);
        searchItem.setEnabled(true);
//        binding.circleMenu.setEnabled(true); // 버튼 활성화
        binding.spinKit.setVisibility(View.GONE); // 로딩 애니메이션 숨김
        updateRecyclerViewWithResponse(photoResponse);
    }
    @Override
    public void onWebServerUploadFailure(String message) {
        MenuItem searchItem = binding.circleMenu.getMenu().findItem(R.id.search);
        searchItem.setEnabled(true);
    }
}

