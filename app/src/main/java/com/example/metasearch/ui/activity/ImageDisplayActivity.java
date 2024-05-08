package com.example.metasearch.ui.activity;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.metasearch.R;
import com.example.metasearch.databinding.ActivityCircleToSearchBinding;
import com.example.metasearch.databinding.ActivityImageDisplayBinding;

import io.github.muddz.styleabletoast.StyleableToast;

public class ImageDisplayActivity extends AppCompatActivity {
    private ActivityImageDisplayBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityImageDisplayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Intent에서 이미지 URI 추출
        String imageUriString = getIntent().getStringExtra("imageUri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            setImageToPhotoView(imageUri);
        } else {
            StyleableToast.makeText(this, "이미지를 불러올 수 없습니다.", R.style.customToast).show();
        }
    }

    private void setImageToPhotoView(Uri imageUri) {
        // PhotoView에 이미지 설정
//        binding.imageViewFullScreen.setImageURI(imageUri);

        // Glide를 사용하여 PhotoView에 이미지 설정
        Glide.with(this)
                .load(imageUri)
                .into(binding.imageViewFullScreen);
    }
}
