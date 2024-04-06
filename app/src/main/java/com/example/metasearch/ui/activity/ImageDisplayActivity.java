package com.example.metasearch.ui.activity;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.metasearch.R;

public class ImageDisplayActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);
        ImageView imageView = findViewById(R.id.imageViewFullScreen);
        String imageUri = getIntent().getStringExtra("imageUri");
        imageView.setImageURI(Uri.parse(imageUri));
    }
}

