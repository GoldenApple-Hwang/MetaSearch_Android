package com.example.metasearch.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.metasearch.R;
import com.example.metasearch.data.dao.DatabaseHelper;
import com.example.metasearch.databinding.ActivityPersonPhotosBinding;
import com.example.metasearch.utils.DatabaseUtils;
import com.example.metasearch.network.interfaces.WebServerPersonDataUploadCallbacks;
import com.example.metasearch.utils.GalleryImageManager;
import com.example.metasearch.manager.WebRequestManager;
import com.example.metasearch.data.model.Person;
import com.example.metasearch.ui.adapter.ImageAdapter;
import com.example.metasearch.ui.adapter.ImageSelectionAdapter;
import com.example.metasearch.ui.viewmodel.ImageViewModel;

import java.util.ArrayList;
import java.util.List;

import io.github.muddz.styleabletoast.StyleableToast;

public class PersonPhotosActivity extends AppCompatActivity
        implements WebServerPersonDataUploadCallbacks,
        ImageAdapter.OnImageClickListener {
    private ImageViewModel imageViewModel;
    private WebRequestManager webRequestManager;
    private ActivityPersonPhotosBinding binding;
    private Integer id;
    private String inputName;
    private byte[] imageData; // 인물 얼굴 사진
    private byte[] thumbnailData;
    private DatabaseHelper databaseHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPersonPhotosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
        setupUI();
        setupListeners(); // 인물 정보 수정 버튼
        loadImages(); // 리사이클러뷰에 관련 인물 사진 모두 출력

    }
    private void init() {
        webRequestManager = WebRequestManager.getWebImageUploader();
        databaseHelper = DatabaseHelper.getInstance(this);

        id = getIntent().getIntExtra("person_id", -1);
        if (id != -1) {
            Person person = databaseHelper.getPersonById(id);
            if (person != null) {
                inputName = person.getInputName();
                imageData = person.getImage();
                thumbnailData = person.getThumbnailImage();
            }
        }
    }
    private void setupUI() {
        // 화면 상단에 인물 이름 출력
        binding.personName.setText(inputName);
        // 썸네일 데이터가 있는 경우 썸네일을 사용, 그렇지 않으면 전체 이미지를 사용
        byte[] displayImageData = (thumbnailData != null && thumbnailData.length > 0) ? thumbnailData : imageData;

        if (displayImageData != null && displayImageData.length > 0) {
            Bitmap imageBitmap = BitmapFactory.decodeByteArray(displayImageData, 0, displayImageData.length);
            binding.face.setImageBitmap(imageBitmap);
        } else {
            binding.face.setImageResource(R.drawable.ic_launcher_foreground); // 기본 이미지 설정
        }
        setupRecyclerView();
    }
    private void loadImages() {
        if (inputName != null) {
            webRequestManager.sendPersonData(inputName, DatabaseUtils.getPersistentDeviceDatabaseName(this), this);
        }
    }
    private void setupRecyclerView() {
        ImageAdapter adapter = new ImageAdapter(new ArrayList<>(), this, this);
        binding.recyclerViewPerson.setLayoutManager(new GridLayoutManager(this, 5));
        binding.recyclerViewPerson.setAdapter(adapter);
    }
    private void setupListeners() {
        // 인물 정보 수정 버튼
        binding.editbtn.setOnClickListener(v -> showEditPersonDialog());
        // 인물 얼굴 클릭 시 썸네일 변경
        binding.face.setOnClickListener(v -> showImageSelectionDialog());
    }
    // 썸네일 변경하는 다이얼로그
    private void showImageSelectionDialog() {
        List<byte[]> images = databaseHelper.getImagesByName(inputName);
        if (images.isEmpty()) {
            StyleableToast.makeText(this, "해당 인물의 이미지가 없습니다.", R.style.customToast).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_image_selection, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerView);

        List<Bitmap> bitmaps = new ArrayList<>();
        for (byte[] image : images) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
            bitmaps.add(bitmap);
        }

        // AlertDialog 변수 선언
        final AlertDialog dialog = builder.create();

        ImageSelectionAdapter adapter = new ImageSelectionAdapter(this, bitmaps, bitmap -> {
            updateThumbnailImage(bitmap);
            dialog.dismiss(); // 다이얼로그 닫기
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);

        builder.setView(dialogView)
//                .setTitle("이미지 선택")
                .setNegativeButton("취소", (dialogInterface, which) -> dialog.dismiss());

        dialog.setView(dialogView);
        dialog.show();
    }
    // 이름이 같은 모든 사람의 썸네일 이미지를 업데이트 하는 메서드
    private void updateThumbnailImage(Bitmap bitmap) {
        if (bitmap != null) {
            // 썸네일 이미지 변경 (기존 IMAGE 필드는 변경하지 않음)
            binding.face.setImageBitmap(bitmap);

            // 새로운 썸네일 이미지를 데이터베이스에 저장 (여기서는 이름을 기준으로 저장)
            boolean updateSuccess = databaseHelper.updateThumbnailImageByName(inputName, GalleryImageManager.getBytes(bitmap));
            if (updateSuccess) {
                StyleableToast.makeText(this, "프로필 사진이 변경되었습니다.", R.style.customToast).show();
            } else {
                StyleableToast.makeText(this, "프로필 사진 변경 실패.", R.style.customToast).show();
            }
        }
    }
    private void showEditPersonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_person, null);

        EditText editPersonName = dialogView.findViewById(R.id.editPersonName);
        EditText editPhoneNumber = dialogView.findViewById(R.id.editPhoneNumber);
        CheckBox checkBoxHomeDisplay = dialogView.findViewById(R.id.checkBoxHomeDisplay); // 체크박스 찾기

        editPersonName.setText(inputName);
        editPhoneNumber.setText(databaseHelper.getPhoneNumberById(id));

        checkBoxHomeDisplay.setChecked(databaseHelper.getHomeDisplayById(id)); // 체크박스 상태 설정

        builder.setView(dialogView)
                .setTitle("인물 정보 수정")
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newPersonName = editPersonName.getText().toString();
            String newPhoneNumber = editPhoneNumber.getText().toString();
            boolean newHomeDisplay = checkBoxHomeDisplay.isChecked(); // 새로운 체크 상태 가져오기

            // 이름 중복 검사
            if (!newPersonName.isEmpty() && databaseHelper.isNameExists(newPersonName) && !newPersonName.equals(inputName)) {
                new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
                        .setTitle("이름 중복")
                        .setMessage("이미 존재하는 이름 입니다. 그래도 저장하시겠습니까?")
                        .setPositiveButton("예", (dialogInterface, i) -> {

                            updatePersonInfo(newPersonName, newPhoneNumber, newHomeDisplay);
                            dialog.dismiss();

                        })
                        .setNegativeButton("아니요", null)
                        .show();
            } else {

                // 이름 중복이 없거나 입력하지 않은 경우
                updatePersonInfo(newPersonName, newPhoneNumber, newHomeDisplay);

                dialog.dismiss();
            }
        });
    }

    private void updatePersonInfo(String newName, String newPhone, boolean newHomeDisplay) {
        boolean updateSuccess = databaseHelper.updatePersonByName(inputName, newName, newPhone, newHomeDisplay);

        if (updateSuccess) {
            StyleableToast.makeText(this, "인물 정보가 저장되었습니다.", R.style.customToast).show();
            webRequestManager.changePersonName(DatabaseUtils.getPersistentDeviceDatabaseName(this), inputName, newName);
            inputName = newName;
            binding.personName.setText(newName);
        } else {
            StyleableToast.makeText(this, "인물 정보 업데이트를 종료합니다.", R.style.customToast).show();
        }
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
            matchedUris = GalleryImageManager.findMatchedUris(photoResponse, this);
        }

        ImageAdapter imageAdapter = new ImageAdapter(matchedUris, this, this);
        binding.recyclerViewPerson.setAdapter(imageAdapter);

        updateUIWithMatchedUris(matchedUris);
    }

    @Override
    public void onPersonDataUploadSuccess(List<String> personImages) {
        runOnUiThread(() -> updateRecyclerViewWithResponse(personImages));
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
