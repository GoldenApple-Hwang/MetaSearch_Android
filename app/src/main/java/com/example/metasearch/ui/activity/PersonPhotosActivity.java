package com.example.metasearch.ui.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.metasearch.databinding.ActivityPersonPhotosBinding;

public class PersonPhotosActivity extends AppCompatActivity {
    private ActivityPersonPhotosBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupUI();

        // 여기에서 personName을 사용하여 사진을 로드하고 표시

        // PersonPhotosActivity 내에서 사진 데이터 로드 후 RecyclerView 설정
//        List<Uri> photoUris = loadPersonPhotos(personName); // 가정: 이 메서드가 사진 URI 리스트를 반환
//        PhotoAdapter photoAdapter = new PhotoAdapter(photoUris, this);
//        recyclerView.setAdapter(photoAdapter);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
    private void setupUI() {
        binding = ActivityPersonPhotosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.personName.setText(getIntent().getStringExtra("personName"));
    }
}