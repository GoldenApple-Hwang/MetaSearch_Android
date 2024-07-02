package com.example.metasearch.ui.viewmodel;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.Manifest;
import android.app.Application;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.helper.DatabaseUtils;
import com.example.metasearch.interfaces.WebServerPersonFrequencyUploadCallbacks;
import com.example.metasearch.manager.WebRequestManager;
import com.example.metasearch.model.Person;
import com.example.metasearch.model.response.PersonFrequencyResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersonViewModel extends AndroidViewModel implements WebServerPersonFrequencyUploadCallbacks {

    private final MutableLiveData<List<Person>> filteredPeopleLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Person>> homeDisplayPeopleLiveData = new MutableLiveData<>();
    private List<Person> allPeople = new ArrayList<>();
    private final DatabaseHelper databaseHelper;
    private final WebRequestManager webRequestManager;

    private String currentFilterQuery = "";
    private int currentSortOption = 0; // 0: Alphabetical, 1: By Photo Count, 2: Home Display

    public PersonViewModel(@NonNull Application application) {
        super(application);
        databaseHelper = DatabaseHelper.getInstance(application.getApplicationContext());
        webRequestManager = WebRequestManager.getWebImageUploader();
        fetchPeopleFromLocalDatabase();
    }

    public LiveData<List<Person>> getPeople() {
        return filteredPeopleLiveData;
    }

    public LiveData<List<Person>> getHomeDisplayPeople() {
        return homeDisplayPeopleLiveData;
    }

    public void fetchPeopleFromLocalDatabase() {
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            allPeople = databaseHelper.getUniquePersons();
            if (allPeople.isEmpty()) {
                Log.e("PersonViewModel", "No persons found in the local database.");
                // 빈 목록 처리를 위한 추가 작업
                filteredPeopleLiveData.setValue(new ArrayList<>());
                homeDisplayPeopleLiveData.setValue(new ArrayList<>());
            } else {
                fetchPersonFrequencies();
            }
        } else {
            Log.e("PersonViewModel", "READ_CALL_LOG permission is not granted.");
            // 권한이 없을 때 빈 목록 설정
            filteredPeopleLiveData.setValue(new ArrayList<>());
            homeDisplayPeopleLiveData.setValue(new ArrayList<>());
        }
    }
    private void normalizeScores(List<Person> people) {
        int maxPhotoCount = 1; // 최소값을 1로 설정하여 나눗셈에서 0을 방지
        long maxTotalDuration = 1; // 최소값을 1로 설정하여 나눗셈에서 0을 방지

        for (Person person : people) {
            if (person.getPhotoCount() > maxPhotoCount) {
                maxPhotoCount = person.getPhotoCount();
            }
            if (person.getTotalDuration() > maxTotalDuration) {
                maxTotalDuration = person.getTotalDuration();
            }
        }

        for (Person person : people) {
            System.out.println(person.getInputName() + "의 총 통화량: " + person.getTotalDuration());
            double normalizedPhotoCount = (double) person.getPhotoCount() / maxPhotoCount;
            double normalizedTotalDuration = (double) person.getTotalDuration() / maxTotalDuration;
            double normalizedScore = (normalizedPhotoCount + normalizedTotalDuration) / 2.0;
            person.setNormalizedScore(normalizedScore);

            // 정규화: 인물 친밀도 랭킹 테스트 로그
            System.out.println(person.getInputName() + "의 친밀도 점수: " + person.getNormalizedScore()
            +person.getPhotoCount() + ", " + person.getTotalDuration());
        }
    }
    private void applyCurrentFilterAndSort() {
        List<Person> filteredList = new ArrayList<>(allPeople);

        // 필터링 적용
        if (!currentFilterQuery.isEmpty()) {
            filteredList = new ArrayList<>();
            for (Person person : allPeople) {
                if (person.getInputName().toLowerCase().contains(currentFilterQuery.toLowerCase())) {
                    filteredList.add(person);
                }
            }
        }

        // 정렬 적용
        switch (currentSortOption) {
            case 0:
                filteredList.sort((p1, p2) -> p1.getInputName().compareToIgnoreCase(p2.getInputName()));
                break;
            case 1:
                filteredList.sort((p1, p2) -> Integer.compare(p2.getPhotoCount(), p1.getPhotoCount()));
                break;
            case 2:
                List<Person> homeDisplayList = new ArrayList<>();
                for (Person person : allPeople) {
                    if (person.isHomeDisplay()) {
                        homeDisplayList.add(person);
                    }
                }
                normalizeScores(homeDisplayList);
                homeDisplayList.sort((p1, p2) -> Double.compare(p2.getNormalizedScore(), p1.getNormalizedScore()));

                homeDisplayPeopleLiveData.setValue(homeDisplayList);
                filteredPeopleLiveData.setValue(homeDisplayList);
                // 홈 화면에 표시할 인물 로그
                for (Person person : homeDisplayList) {
                    Log.d(TAG, "홈 화면에 표시할 인물: " + person.getInputName());
                }
                return;
        }

        filteredPeopleLiveData.setValue(filteredList);
    }
    public void filterPeople(String query) {
        currentFilterQuery = query;
        applyCurrentFilterAndSort();
    }

    public void deletePerson(Person person) {
        databaseHelper.deletePersonByName(person.getInputName());
        fetchPeopleFromLocalDatabase();
    }

    public void sortAlphabetical() {
        currentSortOption = 0;
        applyCurrentFilterAndSort();
    }

    public void sortByPhotoCount() {
        currentSortOption = 1;
        applyCurrentFilterAndSort();
    }

    public void filterHomeScreen() {
        currentSortOption = 2;
        applyCurrentFilterAndSort();
    }

    private void fetchPersonFrequencies() {
        List<Person> persons = databaseHelper.getUniquePersons();
        if (!persons.isEmpty()) {
            webRequestManager.getPersonFrequency(DatabaseUtils.getPersistentDeviceDatabaseName(getApplication()), persons, this);
        }
    }

    @Override
    public void onPersonFrequencyUploadSuccess(PersonFrequencyResponse responses) {
        for (PersonFrequencyResponse.Frequency frequency : responses.getFrequencies()) {
            for (Person person : allPeople) {
                if (person.getInputName().equals(frequency.getPersonName())) {
                    Log.d("RANK", person.getInputName());
                    person.setPhotoCount(frequency.getFrequency());
                }
            }
        }
        applyCurrentFilterAndSort();
    }

    @Override
    public void onPersonFrequencyUploadFailure(String message) {
        // 실패 처리 로직 추가 가능
    }
}
