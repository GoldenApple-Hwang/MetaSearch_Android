package com.example.metasearch.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.metasearch.R;
import com.example.metasearch.databinding.ActivityImageDisplayBinding;

import io.github.muddz.styleabletoast.StyleableToast;

public class ImageDisplayActivity extends AppCompatActivity {
    private ActivityImageDisplayBinding binding;
    private String imageUriString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityImageDisplayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Intent에서 이미지 URI 추출
        imageUriString = getIntent().getStringExtra("imageUri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            setImageToPhotoView(imageUri);
        } else {
            StyleableToast.makeText(this, "이미지를 불러올 수 없습니다.", R.style.customToast).show();
        }

        setListeners();
    }
    public void setListeners() {
        // BottomNavigationView 리스너 설정
        binding.imageMenu.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.comment) {
                // 챗 지피티 사용해 이미지 설명 구현
                // 사진 위에 설명 텍스트를 바로 띄움

                return false;
            } else if (item.getItemId() == R.id.graph) {
                // 그래프 액티비티 실행
                startGraphActivity();
            } else if (item.getItemId() == R.id.search) {
                // 써클 투 써치 액티비티 실행
                startCircleToSearchActivity();
            } else if (item.getItemId() == R.id.share) {
                // 사진 공유 기능 동작
                shareImage();
                return false;
            }
            return true;
        });
    }
    private void startGraphActivity() {

    }
    private void startCircleToSearchActivity() {
        Intent intent = new Intent(this, CircleToSearchActivity.class);
        intent.putExtra("imageUri", imageUriString);
        startActivity(intent);
    }
    private void shareImage() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUriString);
        shareIntent.setType("image/*");
        startActivity(Intent.createChooser(shareIntent, "사진 공유하기"));
    }
    private void setImageToPhotoView(Uri imageUri) {
        // Glide를 사용하여 PhotoView에 이미지 설정
        Glide.with(this)
                .load(imageUri)
                .into(binding.imageViewFullScreen);
    }
}
