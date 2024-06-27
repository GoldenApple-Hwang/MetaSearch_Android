package com.example.metasearch.ui.viewmodel;

import static androidx.core.content.ContentProviderCompat.requireContext;
// 복구 상태
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.interfaces.WebServerDeleteEntityCallbacks;
import com.example.metasearch.interfaces.WebServerPersonFrequencyUploadCallbacks;
import com.example.metasearch.manager.WebRequestManager;
import com.example.metasearch.helper.DatabaseUtils;
import com.example.metasearch.model.Person;
import com.example.metasearch.model.response.PersonFrequencyResponse;

import java.util.ArrayList;
import java.util.List;

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
        allPeople = databaseHelper.getAllPerson();
        fetchPersonFrequencies();
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
                filteredList = new ArrayList<>();
                for (Person person : allPeople) {
                    if (person.isHomeDisplay()) {
                        filteredList.add(person);
                    }
                }
                homeDisplayPeopleLiveData.setValue(filteredList);
                break;
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
        List<Person> persons = databaseHelper.getAllPerson();
        if (!persons.isEmpty()) {
            webRequestManager.getPersonFrequency(DatabaseUtils.getPersistentDeviceDatabaseName(getApplication()), persons, this);
        }
    }

    @Override
    public void onPersonFrequencyUploadSuccess(PersonFrequencyResponse responses) {
        for (PersonFrequencyResponse.Frequency frequency : responses.getFrequencies()) {
            for (Person person : allPeople) {
                if (person.getInputName().equals(frequency.getPersonName())) {
                    person.setPhotoCount(frequency.getFrequency());
                }
            }
        }
        applyCurrentFilterAndSort(); // 최신 데이터를 반영하여 필터링 및 정렬 적용
    }

    @Override
    public void onPersonFrequencyUploadFailure(String message) {
        // 실패 처리 로직 추가 가능
    }
}
