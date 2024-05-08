package com.example.metasearch.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.metasearch.R;
import com.example.metasearch.databinding.ActivityPersonPhotosBinding;
import com.example.metasearch.manager.GalleryImageManager;
import com.example.metasearch.manager.WebRequestManager;
import com.example.metasearch.ui.adapter.ImageAdapter;
import com.example.metasearch.ui.viewmodel.ImageViewModel;

import java.util.ArrayList;
import java.util.List;

import io.github.muddz.styleabletoast.StyleableToast;

public class PersonPhotosActivity extends AppCompatActivity
        implements WebRequestManager.WebServerPersonDataUploadCallbacks,
                    ImageAdapter.OnImageClickListener {
    private ImageViewModel imageViewModel;
    private WebRequestManager webRequestManager;
    private ActivityPersonPhotosBinding binding;
    private String imageName;
    private String userName;
    private byte[] imageData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();
        setupUI();
        loadImages(); // 리사이클러뷰에 관련 인물 사진 모두 출력
    }
    private void loadImages() {
        // 얼굴 DB에는 사진 이름, 바이트 배열, 인물 이름이 저장 되어 있음
        // 사용자가 이름을 재설정 하지 않은 경우, 사진 이름으로 검색
//        if (userName.equals("")) {
//            webRequestManager.sendPersonData(imageName, "youjeong", this);
//        } else { // 유저 이름으로 검색
//            webRequestManager.sendPersonData(userName, "youjeong", this);
//        }

        // Test
        webRequestManager.sendPersonData("사람B","youjeong", this);
    }
    private void setupRecyclerView() {
        ImageAdapter adapter = new ImageAdapter(new ArrayList<>(), this, this);
        binding.recyclerViewPerson.setLayoutManager(new GridLayoutManager(this, 5));
        binding.recyclerViewPerson.setAdapter(adapter);
    }
    private void setupUI() {
        binding = ActivityPersonPhotosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // 화면 상단에 인물 이름 출력
        binding.personName.setText(userName);
        // 바이트 배열을 Bitmap으로 변환
        Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

        binding.face.setImageBitmap(imageBitmap);
        setupRecyclerView();
    }
    private void init() {
        webRequestManager = WebRequestManager.getWebImageUploader();
        imageName = getIntent().getStringExtra("imageName");
        imageData = getIntent().getByteArrayExtra("imageData");
        userName = getIntent().getStringExtra("personName");
    }
    private void updateUIWithMatchedUris(List<Uri> matchedUris) {
        if (imageViewModel == null) {
            imageViewModel = new ViewModelProvider(this).get(ImageViewModel.class);
        }
        imageViewModel.setImageUris(matchedUris);

        if (!matchedUris.isEmpty()) {
            ImageAdapter adapter = (ImageAdapter) binding.recyclerViewPerson.getAdapter();
            if (adapter != null) {
                adapter.updateData(matchedUris);
            }
        } else {
//            Toast.makeText(this, "No matched images found.", Toast.LENGTH_SHORT).show();
            StyleableToast.makeText(this, "관련 사진이 없습니다.", R.style.customToast).show();
        }
    }
    private void updateRecyclerViewWithResponse(List<String> photoResponse) {
        List<Uri> matchedUris = new ArrayList<>();
        if (photoResponse != null) {
            // 이미지 이름 리스트에서 확장자를 제거하지 않고 사용
            matchedUris = GalleryImageManager.findMatchedUris(photoResponse, this);
        }
        System.out.println(matchedUris);

        ImageAdapter imageAdapter = new ImageAdapter(matchedUris, this, this);
        binding.recyclerViewPerson.setAdapter(imageAdapter);

        updateUIWithMatchedUris(matchedUris);
    }
    // UI 업데이트
    @Override
    public void onPersonDataUploadSuccess(List<String> personImages) {
        runOnUiThread(() -> {
            updateRecyclerViewWithResponse(personImages);
        });
    }
    @Override
    public void onPersonDataUploadFailure(String message) {

    }
    @Override
    public void onImageClick(Uri uri) {
        Intent intent = new Intent(this, ImageDisplayActivity.class);
        intent.putExtra("imageUri", uri.toString());
        startActivity(intent);
    }
}