package com.example.metasearch.interfaces;

import java.util.List;

public interface WebServerPersonDataUploadCallbacks {
    void onPersonDataUploadSuccess(List<String> photoNameResponse);
    void onPersonDataUploadFailure(String message);
}