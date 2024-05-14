package com.example.metasearch.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.metasearch.R;
import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.databinding.ActivityPersonPhotosBinding;
import com.example.metasearch.helper.DatabaseUtils;
import com.example.metasearch.manager.AIRequestManager;
import com.example.metasearch.manager.GalleryImageManager;
import com.example.metasearch.manager.WebRequestManager;
import com.example.metasearch.model.Person;
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
    private AIRequestManager aiRequestManager;
    private ActivityPersonPhotosBinding binding;
    private Integer id;
    private String imageName;
    private byte[] imageData;
    private DatabaseHelper databaseHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();
        setupUI();
        setupListeners(); // 인물 정보 수정 버튼
        loadImages(); // 리사이클러뷰에 관련 인물 사진 모두 출력
    }
    private void loadImages() {
        webRequestManager.sendPersonData(imageName, DatabaseUtils.getPersistentDeviceDatabaseName(this), this);
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
        binding.personName.setText(imageName);
        // 바이트 배열을 Bitmap으로 변환
        Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

        binding.face.setImageBitmap(imageBitmap);
        setupRecyclerView();
    }
    private void setupListeners() {
        binding.editbtn.setOnClickListener(v -> showEditPersonDialog());
    }
    private void init() {
        aiRequestManager = AIRequestManager.getAiImageUploader();
        webRequestManager = WebRequestManager.getWebImageUploader();
        databaseHelper = DatabaseHelper.getInstance(this);

        id = getIntent().getIntExtra("id", -1);
        if (id != -1) {
            Person person = databaseHelper.getPersonById(id);
            if (person != null) {
                imageName = person.getImageName();
                imageData = person.getImage();
            }
        }
    }
    // 인물 정보(이름, 전화번호) 수정 다이얼로그
    private void showEditPersonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_person, null);

        EditText editPersonName = dialogView.findViewById(R.id.editPersonName);
        EditText editPhoneNumber = dialogView.findViewById(R.id.editPhoneNumber);

        editPersonName.setText(imageName);
        editPhoneNumber.setText(databaseHelper.getPhoneNumberById(id));

        builder.setView(dialogView)
                .setTitle("인물 정보 수정")
                .setPositiveButton("저장", (dialog, which) -> {
                    String newPersonName = editPersonName.getText().toString();
                    String newPhoneNumber = editPhoneNumber.getText().toString();

                    // 이름 및 전화번호 업데이트
                    if (!newPersonName.isEmpty()) {
                        // 얼굴 DB 업데이트
//                        boolean updateSuccess = databaseHelper.updateUserNameAndPhoneNumber(imageName, newPersonName, newPhoneNumber);
                        boolean updateSuccess = databaseHelper.updatePersonById(id, newPersonName, newPhoneNumber);
                        if (updateSuccess) {
                            // 웹 서버에 이름 변경 요청 보내기
                            webRequestManager.changePersonName(
                                    DatabaseUtils.getPersistentDeviceDatabaseName(this),
                                    imageName, // 현재 이름
                                    newPersonName
                            );
                            // AI 서버에 이름 변경 요청 보내기
                            aiRequestManager.uploadPersonName(
                                    DatabaseUtils.getPersistentDeviceDatabaseName(this),
                                    imageName, // 현재 이름
                                    newPersonName);

                            imageName = newPersonName;
                            binding.personName.setText(newPersonName);
                            StyleableToast.makeText(this, "인물 정보 수정 완료", R.style.customToast);
                        } else {
                            StyleableToast.makeText(this, "Failed to update info.", R.style.customToast).show();
                        }
                    }
                })
                .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
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