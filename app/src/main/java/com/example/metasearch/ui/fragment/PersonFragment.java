package com.example.metasearch.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.metasearch.databinding.FragmentPersonBinding;
import com.example.metasearch.interfaces.ImageAnalysisCompleteListener;
import com.example.metasearch.model.Person;
import com.example.metasearch.ui.adapter.PersonAdapter;
import com.example.metasearch.ui.viewmodel.PersonViewModel;

import java.util.List;

public class PersonFragment extends Fragment implements ImageAnalysisCompleteListener {

    private FragmentPersonBinding binding;
    private PersonViewModel viewModel;
    private PersonAdapter personAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPersonBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViewModel();
        setupRecyclerView();
        setupSearchView();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.fetchPeopleFromLocalDatabase();
    }

    private void setupRecyclerView() {
        personAdapter = new PersonAdapter(requireContext(), viewModel);
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

    private void updateRecyclerView(List<Person> people) {
        personAdapter.setPeople(people);
    }

    @Override
    public void onImageAnalysisComplete() {
        // 이미지 분석 작업이 완료되면 호출되는 콜백
        viewModel.fetchPeopleFromLocalDatabase(); // ViewModel에서 로컬 데이터베이스에서 데이터를 가져오는 메서드 호출
    }
}
