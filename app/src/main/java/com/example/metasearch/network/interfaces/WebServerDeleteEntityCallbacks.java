package com.example.metasearch.network.interfaces;

public interface WebServerDeleteEntityCallbacks {
    void onDeleteEntitySuccess(String message);
    void onDeleteEntityFailure(String message);
}
