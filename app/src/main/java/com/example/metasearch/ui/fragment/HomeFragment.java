package com.example.metasearch.ui.fragment;

import static com.example.metasearch.manager.GalleryImageManager.getAllGalleryImagesUri;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.example.metasearch.R;
import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.databinding.FragmentHomeBinding;
import com.example.metasearch.interfaces.ImageAnalysisCompleteListener;
import com.example.metasearch.interfaces.Update;
import com.example.metasearch.interfaces.WebServerPersonFrequencyUploadCallbacks;
import com.example.metasearch.manager.ImageAnalysisWorker;
import com.example.metasearch.manager.ImageServiceRequestManager;
import com.example.metasearch.manager.WebRequestManager;
import com.example.metasearch.model.Person;
import com.example.metasearch.model.response.PersonFrequencyResponse;
import com.example.metasearch.ui.activity.ImageDisplayActivity;
import com.example.metasearch.ui.activity.MainActivity;
import com.example.metasearch.ui.adapter.ImageAdapter;
import com.example.metasearch.ui.adapter.PersonAdapter;
import com.example.metasearch.ui.viewmodel.PersonViewModel;

import java.util.List;

public class HomeFragment extends Fragment
        implements ImageAdapter.OnImageClickListener, Update, ImageAnalysisCompleteListener,
        WebServerPersonFrequencyUploadCallbacks {

    private FragmentHomeBinding binding;
    private DatabaseHelper databaseHelper;
    private ImageServiceRequestManager imageServiceRequestManager;
    private WebRequestManager webRequestManager;

    private boolean isRecyclerViewVisible = true;
    private PersonAdapter personAdapter;
    private PersonViewModel personViewModel;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        setupListeners();
        setupRecyclerViews();
        setupRecyclerViewToggle();
        setupViewModel();
        loadAllGalleryImages();
    }

    private void init() {
        databaseHelper = DatabaseHelper.getInstance(getContext());
        imageServiceRequestManager = ImageServiceRequestManager.getInstance(getContext(), databaseHelper);
        imageServiceRequestManager.setImageAnalysisCompleteListener(this);
        webRequestManager = WebRequestManager.getWebImageUploader();
    }

    private void setupRecyclerViews() {
        personAdapter = new PersonAdapter(getContext(), personViewModel, true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.personRecyclerViewHorizon.setLayoutManager(layoutManager);
        binding.personRecyclerViewHorizon.setAdapter(personAdapter);
    }

    private void setupRecyclerViewToggle() {
        binding.btn.setOnClickListener(v -> {
            if (isRecyclerViewVisible) {
                binding.personRecyclerViewHorizon.setVisibility(View.GONE);
                binding.btn.setImageResource(R.drawable.icon_down);
            } else {
                binding.personRecyclerViewHorizon.setVisibility(View.VISIBLE);
                binding.btn.setImageResource(R.drawable.icon_up);
            }
            isRecyclerViewVisible = !isRecyclerViewVisible;
        });
    }
    private void setupViewModel() {
        personViewModel = new ViewModelProvider(this).get(PersonViewModel.class);
        personViewModel.getHomeDisplayPeople().observe(getViewLifecycleOwner(), this::updateRecyclerView);
    }

    private void updateRecyclerView(List<Person> people) {
        personAdapter.setPeople(people);
    }
    public void startImageAnalysis() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        WorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(ImageAnalysisWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(requireContext()).enqueue(uploadWorkRequest);
    }

    private void setupListeners() {
        binding.galleryRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

    @Override
    public void onPersonFrequencyUploadFailure(String message) {
//        Log.e("HomeFragment", "Data fetch failed: " + message);
//        StyleableToast.makeText(getContext(), "데이터 불러오기 실패: " + message, R.style.customToast).show();
    }
    public void loadAllGalleryImages() {
        ImageAdapter adapter = new ImageAdapter(getAllGalleryImagesUri(requireContext()), requireContext(), this);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 5);
        binding.galleryRecyclerView.setAdapter(adapter);
        binding.galleryRecyclerView.setLayoutManager(layoutManager);
    }
    @Override
    public void onPersonFrequencyUploadSuccess(PersonFrequencyResponse response) {

    }
    @Override
    public void onImageClick(Uri uri) {
        Intent intent = new Intent(requireContext(), ImageDisplayActivity.class);
        intent.putExtra("imageUri", uri.toString());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        personViewModel.fetchPeopleFromLocalDatabase();
        personViewModel.filterHomeScreen();
        loadAllGalleryImages();
    }

    @Override
    public void performDataUpdate() {
        startImageAnalysis();
    }

    @Override
    public void onImageAnalysisComplete() {

    }
}
