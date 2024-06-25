package com.example.metasearch.ui.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.model.Person;
import java.util.ArrayList;
import java.util.List;

public class PersonViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Person>> peopleLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Person>> filteredPeopleLiveData = new MutableLiveData<>();
    private List<Person> allPeople = new ArrayList<>();
    private final DatabaseHelper databaseHelper;

    public PersonViewModel(@NonNull Application application) {
        super(application);
        databaseHelper = DatabaseHelper.getInstance(application.getApplicationContext());
        fetchPeopleFromLocalDatabase(); // ViewModel 초기화 시 데이터를 로드합니다.
    }

    public LiveData<List<Person>> getPeople() {
        return filteredPeopleLiveData;
    }

    public void fetchPeopleFromLocalDatabase() {
        allPeople = databaseHelper.getAllPerson(); // 추후 수정 필요. 현재는 모든 인물 출력
        peopleLiveData.setValue(allPeople);
        filteredPeopleLiveData.setValue(allPeople);
    }

    public void filterPeople(String query) {
        List<Person> filteredList = new ArrayList<>();
        for (Person person : allPeople) {
            if (person.getInputName().toLowerCase().contains(query.toLowerCase()) || person.getInputName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(person);
            }
        }
        filteredPeopleLiveData.setValue(filteredList);
    }
    public void deletePerson(Person person) {
        databaseHelper.deletePersonByName(person.getInputName());
        fetchPeopleFromLocalDatabase(); // 데이터베이스에서 삭제 후 다시 로드
    }
}
