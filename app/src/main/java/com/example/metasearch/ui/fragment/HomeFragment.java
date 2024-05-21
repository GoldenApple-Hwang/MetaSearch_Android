package com.example.metasearch.ui.fragment;

import static com.example.metasearch.manager.GalleryImageManager.getAllGalleryImagesUri;

import com.example.metasearch.R;
import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.helper.DatabaseUtils;
import com.example.metasearch.interfaces.ImageAnalysisCompleteListener;
import com.example.metasearch.interfaces.Update;
import com.example.metasearch.manager.ImageServiceRequestManager;
import com.example.metasearch.manager.WebRequestManager;
import com.example.metasearch.model.Person;
import com.example.metasearch.model.response.PersonFrequencyResponse;
import com.example.metasearch.ui.activity.ImageDisplayActivity;
import com.example.metasearch.ui.activity.MainActivity;
import com.example.metasearch.ui.adapter.PersonAdapter;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.metasearch.ui.adapter.ImageAdapter;
import com.example.metasearch.databinding.FragmentHomeBinding;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.muddz.styleabletoast.StyleableToast;

public class HomeFragment extends Fragment
        implements ImageAdapter.OnImageClickListener, Update, ImageAnalysisCompleteListener,
        WebRequestManager.WebServerPersonFrequencyUploadCallbacks {
    private FragmentHomeBinding binding;
    private DatabaseHelper databaseHelper;
    //이미지 분석 요청 관리 객체 가져옴
    private ImageServiceRequestManager imageServiceRequestManager;
    private WebRequestManager webRequestManager;
    private Dialog dialog; // 인물 랭킹 다이얼로그

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
        imageServiceRequestManager.setImageAnalysisCompleteListener(this);
        webRequestManager = WebRequestManager.getWebImageUploader();
    }
    public void startImageAnalysis() {
        ExecutorService executor = Executors.newSingleThreadExecutor(); // 단일 스레드를 사용하는 ExecutorService 생성
        executor.submit(() -> {
            try {
                // 이미지 분석 시작
                imageServiceRequestManager.getImagePathsAndUpload();
            } catch (IOException | InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        executor.shutdown(); // 작업을 시작한 후 ExecutorService를 종료합니다. 이는 현재 진행 중인 작업이 완료될 때까지 기다립니다.
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
        binding.rankingBtn.setOnClickListener(v -> showRankingDialog());
    }
    private void showRankingDialog() {
        /*
         * 웹 서버에서 데이터(인물 빈도수) 받아와 출력
         * 인물 정보는 인물 데이터베이스에서 가져와 출력
         */
        dialog = new Dialog(requireContext(), R.style.CustomAlertDialogTheme);
        dialog.setContentView(R.layout.dialog_ranking_table);
        dialog.setTitle("랭킹");

        // 데이터베이스에서 인물 정보(전화번호가 저장된 인물 리스트)를 가져오고, 웹 서버에 빈도수 데이터 요청
        List<Person> persons = databaseHelper.getPersonsByCallDuration();
        Log.d("CALL",persons.toString());
        if (!persons.isEmpty()) {
            webRequestManager.getPersonFrequency(DatabaseUtils.getPersistentDeviceDatabaseName(getContext()), persons, this);
        } else {
            StyleableToast.makeText(getContext(), "인물 정보가 없습니다. 전화번호를 등록해주세요.", R.style.customToast).show();
        }

        dialog.show(); // 다이얼로그를 먼저 표시하고, 데이터가 로드되면 업데이트
    }
    @Override
    public void onPersonFrequencyUploadSuccess(PersonFrequencyResponse response) {
        TableLayout table = dialog.findViewById(R.id.tableLayout);

        Map<String, Long> callDurations = new HashMap<>();
        for (Person person : databaseHelper.getPersonsByCallDuration()) {
            callDurations.put(person.getInputName(), person.getTotalDuration());
        }
        int rank = 1;
        for (PersonFrequencyResponse.Frequency freq : response.getFrequencies()) {
            TableRow row = new TableRow(getContext());

            TextView rankText = new TextView(getContext());
            rankText.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            rankText.setGravity(Gravity.CENTER);
            rankText.setText(String.valueOf(rank++));
            rankText.setPadding(8, 8, 8, 8);
            rankText.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.table_cell_background));

            TextView nameText = new TextView(getContext());
            nameText.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            nameText.setGravity(Gravity.CENTER);
            nameText.setText(freq.getPersonName());
            nameText.setPadding(8, 8, 8, 8);
            nameText.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.table_cell_background));


            TextView freqText = new TextView(getContext());
            freqText.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            freqText.setGravity(Gravity.CENTER);
            freqText.setText(String.valueOf(freq.getFrequency()));
            freqText.setPadding(8, 8, 8, 8);
            freqText.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.table_cell_background));

            TextView durationText = new TextView(getContext());
            durationText.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            durationText.setGravity(Gravity.CENTER);
            Long duration = callDurations.getOrDefault(freq.getPersonName(), 0L);
            durationText.setText(formatDuration(duration));
            durationText.setPadding(8, 8, 8, 8);
            durationText.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.table_cell_background));

            row.addView(rankText);
            row.addView(nameText);
            row.addView(freqText);
            row.addView(durationText);
            table.addView(row);
        }
    }

    @Override
    public void onPersonFrequencyUploadFailure(String message) {
        Log.e("HomeFragment", "Data fetch failed: " + message);
        StyleableToast.makeText(getContext(), "데이터 불러오기 실패: " + message, R.style.customToast).show();
    }
    private String formatDuration(Long durationInSeconds) {
        long hours = durationInSeconds / 3600;
        long minutes = (durationInSeconds % 3600) / 60;
        long seconds = durationInSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
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
        loadFaceImages(); // 항상 최신 데이터 로드
        loadAllGalleryImages(); // 항상 최신 데이터 로드
    }
    @Override
    public void performDataUpdate() {
        startImageAnalysis();
    }

    @Override
    public void onImageAnalysisComplete() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(this::loadFaceImages);
        }
    }
}