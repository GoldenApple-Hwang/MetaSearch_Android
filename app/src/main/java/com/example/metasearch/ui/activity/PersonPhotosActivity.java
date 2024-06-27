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

import com.example.metasearch.R;
import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.databinding.ActivityPersonPhotosBinding;
import com.example.metasearch.helper.DatabaseUtils;
import com.example.metasearch.interfaces.WebServerPersonDataUploadCallbacks;
import com.example.metasearch.manager.GalleryImageManager;
import com.example.metasearch.manager.WebRequestManager;
import com.example.metasearch.model.Person;
import com.example.metasearch.ui.adapter.ImageAdapter;
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
    private byte[] imageData;
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

    private void setupUI() {
        // 화면 상단에 인물 이름 출력
        binding.personName.setText(inputName);
        // 바이트 배열을 Bitmap으로 변환하고 이미지 뷰에 설정
        if (imageData != null && imageData.length > 0) {
            Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            binding.face.setImageBitmap(imageBitmap);
        } else {
            binding.face.setImageResource(R.drawable.ic_launcher_foreground); // 기본 이미지 설정
        }
        setupRecyclerView();
    }

    private void setupListeners() {
        binding.editbtn.setOnClickListener(v -> showEditPersonDialog());
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
            }
        }
    }private void showEditPersonDialog() {
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

            if (!newPersonName.isEmpty() && databaseHelper.isNameExists(newPersonName) && !newPersonName.equals(inputName)) {
                // 이름 중복 경고
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
