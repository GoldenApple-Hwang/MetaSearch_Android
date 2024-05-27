package com.example.metasearch.ui.fragment;

import static com.example.metasearch.manager.GalleryImageManager.getAllGalleryImagesUri;

import com.example.metasearch.R;
import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.helper.DatabaseUtils;
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
import com.example.metasearch.ui.adapter.PersonAdapter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.example.metasearch.ui.adapter.ImageAdapter;
import com.example.metasearch.databinding.FragmentHomeBinding;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.muddz.styleabletoast.StyleableToast;

public class HomeFragment extends Fragment
        implements ImageAdapter.OnImageClickListener, Update, ImageAnalysisCompleteListener,
        WebServerPersonFrequencyUploadCallbacks {

    private FragmentHomeBinding binding;
    private DatabaseHelper databaseHelper;
    //이미지 분석 요청 관리 객체 가져옴
    private ImageServiceRequestManager imageServiceRequestManager;
    private WebRequestManager webRequestManager;
    private Dialog dialog; // 인물 랭킹 다이얼로그
    private static final int PERMISSIONS_REQUEST_READ_CALL_LOG = 102;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showRankingDialog();
                } else {
                    showPermissionDeniedDialog();
                }
            });

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
        imageServiceRequestManager = ImageServiceRequestManager.getInstance(getContext(), databaseHelper);
        imageServiceRequestManager.setImageAnalysisCompleteListener(this);
        webRequestManager = WebRequestManager.getWebImageUploader();
    }

    public void startImageAnalysis() {
//        WorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(ImageAnalysisWorker.class)
//                .build();
//
//        WorkManager
//                .getInstance(getContext())
//                .enqueue(uploadWorkRequest);
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // 네트워크 연결 상태를 고려하지 않음
                .build();

        WorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(ImageAnalysisWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(requireContext()).enqueue(uploadWorkRequest);

//        ExecutorService executor = Executors.newSingleThreadExecutor(); // 단일 스레드를 사용하는 ExecutorService 생성
//        executor.submit(() -> {
//            try {
//                // 이미지 분석 시작
//                imageServiceRequestManager.getImagePathsAndUpload();
//            } catch (IOException | InterruptedException | ExecutionException e) {
//                throw new RuntimeException(e);
//            }
//        });
//        executor.shutdown(); // 작업을 시작한 후 ExecutorService를 종료합니다. 이는 현재 진행 중인 작업이 완료될 때까지 기다립니다.
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
        binding.rankingBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALL_LOG)
                    != PackageManager.PERMISSION_GRANTED) {
                // 권한이 없으면 요청
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG);
                } else {
                    requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG}, PERMISSIONS_REQUEST_READ_CALL_LOG);
                }
            } else {
                // 권한이 있으면 랭킹 다이얼로그 출력
                showRankingDialog();
            }
        });
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
        Log.d("CALL", persons.toString());
        if (!persons.isEmpty()) {
            webRequestManager.getPersonFrequency(DatabaseUtils.getPersistentDeviceDatabaseName(getContext()), persons, this);
        } else {
            StyleableToast.makeText(getContext(), "인물 정보가 없습니다. 전화번호를 등록해주세요.", R.style.customToast).show();
        }

        dialog.show(); // 다이얼로그를 먼저 표시하고, 데이터가 로드되면 업데이트
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogTheme)
                .setTitle("권한 요청")
                .setMessage("통화 기록 권한이 필요합니다. 앱 설정에서 권한을 허용해주세요.")
                .setPositiveButton("설정으로 이동", (dialog, which) -> openAppSettings())
                .setNegativeButton("취소", null)
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CALL_LOG) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용되었으면 다시 다이얼로그 표시 메서드 호출
                showRankingDialog();
            } else {
                // 권한이 거부되었을 때 처리
                showPermissionDeniedDialog();
            }
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
        binding.galleryRecyclerView.setAdapter(adapter);
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
    public void onPersonFrequencyUploadSuccess(PersonFrequencyResponse response) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (PersonFrequencyResponse.Frequency freq : response.getFrequencies()) {
            frequencies.put(freq.getPersonName(), freq.getFrequency());
        }

        List<Person> persons = databaseHelper.getPersonsByCallDuration();
        Map<String, Long> callDurations = new HashMap<>();
        for (Person person : persons) {
            callDurations.put(person.getInputName(), person.getTotalDuration());
        }

        // 정규화 위해 최댓값 찾기
        double maxCallDuration = Collections.max(callDurations.values());
        double maxFrequency = Collections.max(frequencies.values());

        // 총점 계산
        Map<String, Double> scores = new HashMap<>();
        for (Person person : persons) {
            double normalizedCallDuration = maxCallDuration != 0 ? callDurations.get(person.getInputName()) / maxCallDuration : 0;
            double normalizedFrequency = maxFrequency != 0 ? frequencies.getOrDefault(person.getInputName(), 0) / maxFrequency : 0;
            double score = (normalizedCallDuration + normalizedFrequency) / 2;
            scores.put(person.getInputName(), score);
        }

        // 점수가 높은 인물 순으로 정렬
        Collections.sort(persons, (p1, p2) -> scores.get(p2.getInputName()).compareTo(scores.get(p1.getInputName())));

        // Update UI
        updateRankingTable(persons, scores, frequencies, callDurations);
    }

    private void updateRankingTable(List<Person> persons, Map<String, Double> scores, Map<String, Integer> frequencies, Map<String, Long> callDurations) {
        TableLayout table = dialog.findViewById(R.id.tableLayout);
        table.removeAllViews();

        // 테이블 헤더 추가
        TableRow header = new TableRow(getContext());
        addTableCell(header, "이름", true);
        addTableCell(header, "사진", true);
        addTableCell(header, "통화", true);
        addTableCell(header, "친밀도", true);
        table.addView(header);

        // 테이블에 데이터 추가
        for (Person person : persons) {
            TableRow row = new TableRow(getContext());
            row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            TextView nameView = new TextView(getContext());
            nameView.setText(person.getInputName());
            nameView.setGravity(Gravity.CENTER);

            TextView frequencyView = new TextView(getContext());
            frequencyView.setText(String.valueOf(frequencies.getOrDefault(person.getInputName(), 0)));
            frequencyView.setGravity(Gravity.CENTER);

            TextView durationView = new TextView(getContext());
            Long durationInSeconds = callDurations.getOrDefault(person.getInputName(), 0L);
            durationView.setText(formatDuration(durationInSeconds));
            durationView.setGravity(Gravity.CENTER);

            TextView scoreView = new TextView(getContext());
            scoreView.setText(String.format(Locale.US, "%.2f", scores.get(person.getInputName())));
            scoreView.setGravity(Gravity.CENTER);

            row.addView(nameView);
            row.addView(frequencyView);
            row.addView(durationView);
            row.addView(scoreView);

            table.addView(row);
        }
    }

    private void addTableCell(TableRow row, String text, boolean isHeader) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(5, 10, 5, 10);
        if (isHeader) {
            tv.setTypeface(null, Typeface.BOLD);
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.pink_grey));
        }
        row.addView(tv);
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
