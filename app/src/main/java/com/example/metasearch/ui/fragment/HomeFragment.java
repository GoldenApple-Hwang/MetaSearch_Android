package com.example.metasearch.ui.fragment;

import static com.example.metasearch.manager.GalleryImageManager.getAllGalleryImagesUri;

import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.model.Person;
import com.example.metasearch.ui.activity.MainActivity;
import com.example.metasearch.ui.adapter.PersonAdapter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.metasearch.ui.adapter.ImageAdapter;
import com.example.metasearch.ui.activity.CircleToSearchActivity;
import com.example.metasearch.databinding.FragmentHomeBinding;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class HomeFragment extends Fragment implements ImageAdapter.OnImageClickListener {
    private FragmentHomeBinding binding;
    private DatabaseHelper databaseHelper;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        databaseHelper = DatabaseHelper.getInstance(requireContext());

        binding.galleryRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    if (dy > 0) {
                        // 스크롤 내릴 때, 네비게이션 바 숨기기
                        activity.hideBottomNavigationView();
                    } else if (dy < 0) {
                        // 스크롤 올릴 때, 네비게이션 바 보이기
                        activity.showBottomNavigationView();
                    }
                }
            }
        });
        loadFaceImages(); // 가로 방향 RecyclerView(인물 얼굴과 이름) 로드

        // 갤러리의 모든 사진을 출력하는 세로 방향 RecyclerView 세팅
        ImageAdapter adapter = new ImageAdapter(getAllGalleryImagesUri(requireContext()), requireContext(), this);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 5);
        binding.galleryRecyclerView.setAdapter(adapter) ;
        binding.galleryRecyclerView.setLayoutManager(layoutManager);

        return root;
    }
    private void loadFaceImages() {
        List<Person> people = databaseHelper.getAllPerson();

        PersonAdapter adapter = new PersonAdapter(people, this, getContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.personRecyclerViewHorizon.setLayoutManager(layoutManager);
        binding.personRecyclerViewHorizon.setAdapter(adapter);
    }
    @Override
    public void onImageClick(Uri uri) {
        Intent intent = new Intent(requireContext(), CircleToSearchActivity.class);
        intent.putExtra("imageUri", uri.toString());
        startActivity(intent);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
