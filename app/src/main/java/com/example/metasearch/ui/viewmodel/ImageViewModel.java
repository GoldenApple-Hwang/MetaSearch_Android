package com.example.metasearch.ui.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class ImageViewModel extends ViewModel {
    private MutableLiveData<List<Uri>> imageUrisLiveData = new MutableLiveData<>();

    // 이미지 URI 리스트를 설정하는 메서드
    public void setImageUris(List<Uri> imageUris) {
        imageUrisLiveData.setValue(imageUris);
    }

    // 이미지 URI 리스트를 반환하는 메서드
    public LiveData<List<Uri>> getImageUris() {
        return imageUrisLiveData;
    }
}
