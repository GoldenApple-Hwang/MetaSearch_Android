package com.example.metasearch.ui.fragment;

import static com.example.metasearch.manager.GalleryImageManager.getAllGalleryImagesUri;

import com.example.metasearch.R;
import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.manager.ImageServiceRequestManager;
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
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.metasearch.ui.adapter.ImageAdapter;
import com.example.metasearch.ui.activity.CircleToSearchActivity;
import com.example.metasearch.databinding.FragmentHomeBinding;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class HomeFragment extends Fragment implements ImageAdapter.OnImageClickListener {
    private FragmentHomeBinding binding;
    //데이터베이스 관리 객체 가져옴
    private DatabaseHelper databaseHelper = DatabaseHelper.getInstance(requireContext());
    //이미지 분석 요청 관리 객체 가져옴
    private ImageServiceRequestManager imageServiceRequestManager = ImageServiceRequestManager.getInstance(requireContext(),databaseHelper);

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Button imageAnalyzeBtn = root.findViewById(R.id.imageAnalyzeBtn);
        imageAnalyzeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //이미지 분석 시작
                    imageServiceRequestManager.getImagePathsAndUpload();
                } catch (IOException | InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        });


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
        loadFaceImages(); // 홈 화면 상단에 가로 방향 RecyclerView(인물 얼굴과 이름) 로드
        loadAllGalleryImages(); // 홈 화면 하단에 세로 방향 RecyclerView(갤러리 모든 사진) 로드

        return root;
    }
    public void loadAllGalleryImages() {
        // 갤러리의 모든 사진을 출력하는 세로 방향 RecyclerView 세팅
        ImageAdapter adapter = new ImageAdapter(getAllGalleryImagesUri(requireContext()), requireContext(), this);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 5);
        binding.galleryRecyclerView.setAdapter(adapter) ;
        binding.galleryRecyclerView.setLayoutManager(layoutManager);
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
