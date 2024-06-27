package com.example.metasearch.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.metasearch.R;
import com.example.metasearch.databinding.FragmentPersonBinding;
import com.example.metasearch.interfaces.ImageAnalysisCompleteListener;
import com.example.metasearch.model.Person;
import com.example.metasearch.ui.activity.MainActivity;
import com.example.metasearch.ui.adapter.PersonAdapter;
import com.example.metasearch.ui.viewmodel.PersonViewModel;

import java.util.List;

public class PersonFragment extends Fragment implements ImageAnalysisCompleteListener {

    private FragmentPersonBinding binding;
    private PersonViewModel viewModel;
    private PersonAdapter personAdapter;

    private static final String SPINNER_SELECTED_POSITION = "spinner_selected_position";
    private int spinnerSelectedPosition = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPersonBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            spinnerSelectedPosition = savedInstanceState.getInt(SPINNER_SELECTED_POSITION, 0);
        }
        setupViewModel();
        setupRecyclerView();
        setupListeners();
        setupSearchView();
        setupSpinner();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SPINNER_SELECTED_POSITION, binding.spinnerFilter.getSelectedItemPosition());
    }


    @Override
    public void onResume() {
        super.onResume();

        viewModel.fetchPeopleFromLocalDatabase();

        binding.spinnerFilter.setSelection(spinnerSelectedPosition);
    }

    private void setupRecyclerView() {
        personAdapter = new PersonAdapter(requireContext(), viewModel, false);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 3);
        binding.personRecyclerView.setLayoutManager(gridLayoutManager);
        binding.personRecyclerView.setAdapter(personAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(PersonViewModel.class);
        viewModel.getPeople().observe(getViewLifecycleOwner(), this::updateRecyclerView);
    }

    private void setupSearchView() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.filterPeople(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.filterPeople(newText);
                return false;
            }
        });
    }
    private void setupListeners() {
        binding.personRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    if (dy > 0) {
                        activity.hideBottomNavigationView();
                    } else if (dy < 0) {
                        activity.showBottomNavigationView();
                    }
                }
            }
        });
    }
    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.filter_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(adapter);

        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinnerSelectedPosition = position;
                applySpinnerSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // 스피너 상태 복원
        binding.spinnerFilter.setSelection(spinnerSelectedPosition);
    }
    private void applySpinnerSelection(int position) {
        switch (position) {
            case 0:
                viewModel.sortAlphabetical();
                break;
            case 1:
                viewModel.sortByPhotoCount();
                break;
            case 2:
                viewModel.filterHomeScreen();
                break;
        }
    }
    private void updateRecyclerView(List<Person> people) {
        personAdapter.setPeople(people);
    }

    @Override
    public void onImageAnalysisComplete() {
        // 이미지 분석 작업이 완료되면 호출되는 콜백
        viewModel.fetchPeopleFromLocalDatabase(); // ViewModel에서 로컬 데이터베이스에서 데이터를 가져오는 메서드 호출
    }
}
