package com.example.metasearch.ui.fragment;

import static com.example.metasearch.manager.GalleryImageManager.getAllGalleryImagesUri;

import com.example.metasearch.R;
import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.interfaces.Update;
import com.example.metasearch.manager.ImageServiceRequestManager;
import com.example.metasearch.model.Person;
import com.example.metasearch.ui.activity.MainActivity;
import com.example.metasearch.ui.adapter.PersonAdapter;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

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

public class HomeFragment extends Fragment
        implements ImageAdapter.OnImageClickListener, Update {
    private FragmentHomeBinding binding;
    //데이터베이스 관리 객체 가져옴
    private DatabaseHelper databaseHelper;
    //이미지 분석 요청 관리 객체 가져옴
    private ImageServiceRequestManager imageServiceRequestManager;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        init();
        setupListeners();
        loadFaceImages(); // 홈 화면 상단에 가로 방향 RecyclerView(인물 얼굴과 이름) 로드
        loadAllGalleryImages(); // 홈 화면 하단에 세로 방향 RecyclerView(갤러리 모든 사진) 로드

        return root;
    }
    private void init() {
        // `requireContext()`를 사용하는 대신 `getContext()` 사용
        databaseHelper = DatabaseHelper.getInstance(getContext());
        imageServiceRequestManager = ImageServiceRequestManager.getInstance(getContext(),databaseHelper);
    }
    public void startImageAnalysis() {
        try {
            //이미지 분석 시작
            imageServiceRequestManager.getImagePathsAndUpload();
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
    private void setupListeners() {
        // 리사이클러뷰 스크롤에 따라 하단의 네비바 높이 조절
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
        // 화면 상단의 정보 아이콘 클릭 시 랭킹 다이얼로그 출력
        binding.rankingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 다이얼로그 생성
                Dialog dialog = new Dialog(requireContext(), R.style.CustomAlertDialogTheme);
                dialog.setContentView(R.layout.dialog_ranking_table);
                dialog.setTitle("정보");

                /*
                * 테이블 데이터를 동적으로 추가하는 테스트 코드
                * 추후 웹 서버에서 데이터 받아와서 출력해야 함
                * 인물 정보는 인물 데이터베이스에서 가져와야 함
                                                  */
                TableLayout table = dialog.findViewById(R.id.tableLayout);
                for (int i = 0; i < 10; i++) {
                    TableRow row = new TableRow(HomeFragment.this.getContext());
                    row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

                    TextView text1 = new TextView(HomeFragment.this.getContext());
                    text1.setText("행 " + i);
                    text1.setPadding(8, 8, 8, 8);

                    TextView text2 = new TextView(HomeFragment.this.getContext());
                    text2.setText("데이터 " + i);
                    text2.setPadding(8, 8, 8, 8);
                    TextView text3 = new TextView(HomeFragment.this.getContext());
                    text3.setText("데이터 " + i);
                    text3.setPadding(8, 8, 8, 8);

                    row.addView(text1);
                    row.addView(text2);
                    row.addView(text3);
                    table.addView(row);
                }

                dialog.show();
            }
        });
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
        if (people.isEmpty()) {
            Log.d("HomeFragment", "No face images found.");
            return;
        }
        Log.d("inputName", people.toString());
        PersonAdapter adapter = new PersonAdapter(people, this, getContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.personRecyclerViewHorizon.setLayoutManager(layoutManager);
        binding.personRecyclerViewHorizon.setAdapter(adapter);
    }
    // 하단의 갤러리 사진 클릭 시, 써클 투 써치로 전환
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
    @Override
    public void onResume() {
        super.onResume();
        loadFaceImages(); // 항상 최신 데이터 로드
        loadAllGalleryImages(); // 항상 최신 데이터 로드
    }
    @Override
    public void performDataUpdate() {
        startImageAnalysis();
    }
}
