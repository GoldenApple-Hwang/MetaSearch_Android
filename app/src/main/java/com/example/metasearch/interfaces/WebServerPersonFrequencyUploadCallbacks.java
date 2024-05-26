package com.example.metasearch.interfaces;

import com.example.metasearch.model.response.PersonFrequencyResponse;

public interface WebServerPersonFrequencyUploadCallbacks {
    void onPersonFrequencyUploadSuccess(PersonFrequencyResponse responses);
    void onPersonFrequencyUploadFailure(String message);
}