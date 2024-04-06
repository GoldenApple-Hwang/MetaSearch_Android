package com.example.metasearch.ui.activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.metasearch.R;

public class PersonPhotosActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_photos);
        String personName = getIntent().getStringExtra("personName");

        TextView nameTextView = findViewById(R.id.personName);
        nameTextView.setText(personName);

        // 여기에서 personName을 사용하여 사진을 로드하고 표시

        // PersonPhotosActivity 내에서 사진 데이터 로드 후 RecyclerView 설정
//        List<Uri> photoUris = loadPersonPhotos(personName); // 가정: 이 메서드가 사진 URI 리스트를 반환
//        PhotoAdapter photoAdapter = new PhotoAdapter(photoUris, this);
//        recyclerView.setAdapter(photoAdapter);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
}