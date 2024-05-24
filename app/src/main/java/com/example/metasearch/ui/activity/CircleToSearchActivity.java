package com.example.metasearch.ui.activity;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.metasearch.R;
import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.databinding.ActivityCircleToSearchBinding;
import com.example.metasearch.helper.DatabaseUtils;
import com.example.metasearch.interfaces.CircleDataUploadCallbacks;
import com.example.metasearch.interfaces.WebServerUploadCallbacks;
import com.example.metasearch.manager.AIRequestManager;
import com.example.metasearch.manager.GalleryImageManager;
import com.example.metasearch.manager.WebRequestManager;
import com.example.metasearch.model.Circle;
import com.example.metasearch.model.response.PhotoResponse;
import com.example.metasearch.ui.adapter.ImageAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.muddz.styleabletoast.StyleableToast;
import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog;
import me.jfenn.colorpickerdialog.interfaces.OnColorPickedListener;

public class CircleToSearchActivity extends AppCompatActivity
        implements ImageAdapter.OnImageClickListener,
        CircleDataUploadCallbacks,
        WebServerUploadCallbacks {
    private ActivityCircleToSearchBinding binding;
    private Uri imageUri;
    private AIRequestManager aiRequestManager;
    private WebRequestManager webRequestManager;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StyleableToast.makeText(this, "드래그 해서 원을 그려주세요.", R.style.customToast).show();
        setupUI();
        setupListeners();
        aiRequestManager = AIRequestManager.getAiImageUploader(this);
        webRequestManager = WebRequestManager.getWebImageUploader();
    }

    private void setupUI() {
        binding = ActivityCircleToSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
        // Glide를 사용하여 이미지 로드 및 표시(이미지 자동 회전 방지)
        Glide.with(this)
                .load(imageUri)
                .into(binding.customImageView);

        // 화면 크기의 1/2 높이로 CustomImageView 크기 조정
        int halfScreenHeight = getResources().getDisplayMetrics().heightPixels / 2;
        ViewGroup.LayoutParams layoutParams = binding.customImageView.getLayoutParams();
        layoutParams.height = halfScreenHeight;
        binding.customImageView.setLayoutParams(layoutParams);
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
                        // AI 서버로 이미지와 원 리스트 전송
                        sendCirclesAndImage();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                } else if (itemId == R.id.reset) {
                    // 리셋 버튼 클릭 시 모든 원 삭제
                    binding.customImageView.clearCircles();
                    // 화면 초기화
                    binding.individualPhotosContainer.removeAllViews();
                    return true;
                } else if (itemId == R.id.color) {
                    // 컬러 버튼 클릭 시 컬러 피커 다이얼로그 표시
                    showColorPickerDialog();
                    return true;
                }
                return false;
            }
        });
    }

    private void sendCirclesAndImage() throws IOException {
        List<Circle> circles = binding.customImageView.getCircles();
        if (imageUri != null && !circles.isEmpty()) {
            // 검색 버튼 비활성화
            MenuItem searchItem = binding.circleMenu.getMenu().findItem(R.id.search);
            searchItem.setEnabled(false);
            // 요청 전에 로딩 아이콘 표시
            binding.spinKit.setVisibility(View.VISIBLE);

            // AI Server로 이미지와 원 리스트 전송
            aiRequestManager.uploadCircleData(imageUri, circles, DatabaseUtils.getPersistentDeviceDatabaseName(this), this, this);
        } else {
            StyleableToast.makeText(this, "이미지 또는 원 정보가 없습니다. 드래그 해서 원을 그려주세요.", R.style.customToast).show();
            // Individual photos 처리
            binding.individualPhotosContainer.removeAllViews();
        }
    }
    private void updateRecyclerViewWithResponse(PhotoResponse photoResponse) {
        Set<String> uniqueCommonPhotoNames = new HashSet<>();
        Map<String, List<String>> individualPhotosMap = photoResponse.getPhotos().getIndividualPhotos();

        // Common photos 처리
        if (photoResponse != null && photoResponse.getPhotos() != null) {
            uniqueCommonPhotoNames.addAll(photoResponse.getPhotos().getCommonPhotos());
        }
        List<Uri> commonPhotoUris = GalleryImageManager.findMatchedUris(new ArrayList<>(uniqueCommonPhotoNames), this);

        // Individual photos 처리
        binding.individualPhotosContainer.removeAllViews();

        // Common photos 추가
        if (!commonPhotoUris.isEmpty()) {
            TextView commonPhotosTextView = new TextView(this);
            commonPhotosTextView.setText("Common Photos");
            commonPhotosTextView.setTextSize(16);
            Typeface customFont = ResourcesCompat.getFont(this, R.font.light); // 폰트 로드
            commonPhotosTextView.setTypeface(customFont, Typeface.BOLD); // 폰트와 스타일 적용

            commonPhotosTextView.setPadding(16, 16, 16, 16);
            binding.individualPhotosContainer.addView(commonPhotosTextView);

            // Common Photos RecyclerView 추가
            RecyclerView commonRecyclerView = new RecyclerView(this);
            commonRecyclerView.setLayoutManager(new GridLayoutManager(this, 5));
            ImageAdapter commonAdapter = new ImageAdapter(commonPhotoUris, this, this);
            commonRecyclerView.setAdapter(commonAdapter);
            binding.individualPhotosContainer.addView(commonRecyclerView);
        }

        // Common photos를 Individual photos에서 제거
        Set<String> commonPhotoNames = new HashSet<>(uniqueCommonPhotoNames);

        for (Map.Entry<String, List<String>> entry : individualPhotosMap.entrySet()) {
            String category = entry.getKey();
            List<String> photoNames = entry.getValue();
            // Common photos에 포함되지 않은 사진만 필터링
            List<String> filteredPhotoNames = new ArrayList<>();
            for (String photoName : photoNames) {
                if (!commonPhotoNames.contains(photoName)) {
                    filteredPhotoNames.add(photoName);
                }
            }
            List<Uri> photoUris = GalleryImageManager.findMatchedUris(filteredPhotoNames, this);

            if (!photoUris.isEmpty()) {
                // Category TextView 추가
                TextView categoryTextView = new TextView(this);
                categoryTextView.setText("#" + category);
                categoryTextView.setTextSize(16);
                Typeface customFont = ResourcesCompat.getFont(this, R.font.light); // 폰트 로드
                categoryTextView.setTypeface(customFont, Typeface.BOLD); // 폰트와 스타일 적용
                categoryTextView.setPadding(16, 16, 16, 16);
                binding.individualPhotosContainer.addView(categoryTextView);

                // Photos RecyclerView 추가
                RecyclerView recyclerView = new RecyclerView(this);
                recyclerView.setLayoutManager(new GridLayoutManager(this, 5));
                ImageAdapter adapter = new ImageAdapter(photoUris, this, this);
                recyclerView.setAdapter(adapter);
                binding.individualPhotosContainer.addView(recyclerView);
            }
        }
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
            try {
                MenuItem searchItem = binding.circleMenu.getMenu().findItem(R.id.search);
                searchItem.setEnabled(true); // 검색 버튼 활성화
                binding.spinKit.setVisibility(View.GONE); // 로딩 아이콘 숨김
                if (detectedObjects.isEmpty()) {
                    StyleableToast.makeText(this, "분석된 객체가 없습니다.", R.style.customToast).show();
                    // Individual photos 처리
                    binding.individualPhotosContainer.removeAllViews();
                } else {
                    List<String> newDetectedObjects = new ArrayList<>();
                    for (String imageName : detectedObjects) {
                        String inputName = DatabaseHelper.getInstance(context).getInputNameByImageName(imageName);
                        if (inputName != null) {
                            newDetectedObjects.add(inputName);
                        } else {
                            newDetectedObjects.add(imageName); // 매칭되지 않으면 원래 이름을 사용
                        }
                    }
                    // Web Server로 이미지 분석 결과 전송
                    webRequestManager.sendDetectedObjectsToWebServer(newDetectedObjects, DatabaseUtils.getPersistentDeviceDatabaseName(this), this);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating UI: ", e);
                StyleableToast.makeText(this, "분석된 객체가 없습니다.", R.style.customToast).show();
                // Individual photos 처리
                binding.individualPhotosContainer.removeAllViews();
            }
        });
    }

    @Override
    public void onCircleUploadFailure(String message) {
        runOnUiThread(() -> {
            MenuItem searchItem = binding.circleMenu.getMenu().findItem(R.id.search);
            searchItem.setEnabled(true); // 검색 버튼 비활성화
            binding.spinKit.setVisibility(View.GONE); // 로딩 아이콘 숨김
            StyleableToast.makeText(this, "Upload failed: " + message, R.style.customToast).show();
        });
    }

    @Override
    public void onWebServerUploadSuccess(PhotoResponse photoResponse) {
        MenuItem searchItem = binding.circleMenu.getMenu().findItem(R.id.search);
        searchItem.setEnabled(true); // 검색 버튼 활성화
        binding.spinKit.setVisibility(View.GONE); // 로딩 아이콘 숨김
        updateRecyclerViewWithResponse(photoResponse); // 검색된 사진으로 화면 업데이트
    }

    @Override
    public void onWebServerUploadFailure(String message) {
        MenuItem searchItem = binding.circleMenu.getMenu().findItem(R.id.search);
        searchItem.setEnabled(true); // 검색 버튼 활성화
    }
}
