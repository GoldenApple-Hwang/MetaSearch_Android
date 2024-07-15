package com.example.metasearch.network.interfaces;

import com.example.metasearch.network.response.PersonFrequencyResponse;

public interface WebServerPersonFrequencyUploadCallbacks {
    void onPersonFrequencyUploadSuccess(PersonFrequencyResponse responses);
    void onPersonFrequencyUploadFailure(String message);
}