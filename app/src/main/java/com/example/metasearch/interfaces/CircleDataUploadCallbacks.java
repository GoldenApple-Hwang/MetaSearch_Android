package com.example.metasearch.interfaces;

import java.util.List;

public interface CircleDataUploadCallbacks {
    void onCircleUploadSuccess(List<String> detectedObjects);
    void onCircleUploadFailure(String message);
}