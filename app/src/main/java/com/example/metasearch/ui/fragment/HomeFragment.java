package com.example.metasearch.ui.fragment;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;
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
import java.util.ArrayList;
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
    private ImageServiceRequestManager imageServiceRequestManager;
    private WebRequestManager webRequestManager;
    private Dialog dialog;
    private static final int PERMISSIONS_REQUEST_READ_CALL_LOG = 102;
    private PersonAdapter personAdapter;

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
        setupRecyclerViews();
        loadFaceImages();
        loadAllGalleryImages();

        return root;
    }

    private void init() {
        databaseHelper = DatabaseHelper.getInstance(getContext());
        imageServiceRequestManager = ImageServiceRequestManager.getInstance(getContext(), databaseHelper);
        imageServiceRequestManager.setImageAnalysisCompleteListener(this);
        webRequestManager = WebRequestManager.getWebImageUploader();
    }

    private void setupRecyclerViews() {
        personAdapter = new PersonAdapter(Collections.emptyList(), this, getContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.personRecyclerViewHorizon.setLayoutManager(layoutManager);
        binding.personRecyclerViewHorizon.setAdapter(personAdapter);
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

        binding.rankingBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALL_LOG)
                    != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG);
                } else {
                    requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG}, PERMISSIONS_REQUEST_READ_CALL_LOG);
                }
            } else {
                showRankingDialog();
            }
        });
    }

    private void showRankingDialog() {
        dialog = new Dialog(requireContext(), R.style.CustomAlertDialogTheme);
        dialog.setContentView(R.layout.dialog_ranking_table);
        dialog.setTitle("랭킹");

        List<Person> persons = databaseHelper.getAllPersonExceptMe();
        Log.d("CALL", persons.toString());
        if (!persons.isEmpty()) {
            webRequestManager.getPersonFrequency(DatabaseUtils.getPersistentDeviceDatabaseName(getContext()), persons, this);
        } else {
            StyleableToast.makeText(getContext(), "인물 정보가 없습니다. 전화번호를 등록해주세요.", R.style.customToast).show();
        }

        dialog.show();
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
                showRankingDialog();
            } else {
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
        personAdapter.updateData(people);
    }

    private void updateFaceImages(List<Person> persons) {
        personAdapter.updateData(persons);
    }

    @Override
    public void onPersonFrequencyUploadSuccess(PersonFrequencyResponse response) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (PersonFrequencyResponse.Frequency freq : response.getFrequencies()) {
            frequencies.put(freq.getPersonName(), freq.getFrequency());
        }

        List<Person> persons = databaseHelper.getAllPersonExceptMe();
        Map<String, Long> callDurations = new HashMap<>();
        for (Person person : persons) {
            callDurations.put(person.getInputName(), person.getTotalDuration());
            Log.d(TAG, "Person: " + person.getInputName() + ", Total Duration: " + person.getTotalDuration());
        }

        double maxCallDuration = callDurations.isEmpty() ? 0 : Collections.max(callDurations.values());
        double maxFrequency = frequencies.isEmpty() ? 0 : Collections.max(frequencies.values());

        Map<String, Double> scores = new HashMap<>();
        for (Person person : persons) {
            double normalizedCallDuration = maxCallDuration != 0 ? callDurations.get(person.getInputName()) / maxCallDuration : 0;
            double normalizedFrequency = maxFrequency != 0 ? frequencies.getOrDefault(person.getInputName(), 0) / maxFrequency : 0;
            double score = (normalizedCallDuration + normalizedFrequency) / 2;
            scores.put(person.getInputName(), score);
            Log.d(TAG, "Person: " + person.getInputName() + ", Score: " + score);
        }

        Collections.sort(persons, (p1, p2) -> scores.get(p2.getInputName()).compareTo(scores.get(p1.getInputName())));

        // '나'라는 인물을 맨 앞에 추가
        List<Person> sortedPersons = new ArrayList<>();
        for (Person person : persons) {
            if (person.getInputName().equals("나")) {
                sortedPersons.add(0, person);
            } else {
                sortedPersons.add(person);
            }
        }

        updateRankingTable(sortedPersons, scores, frequencies, callDurations);
        updateFaceImages(sortedPersons);
    }

    private void updateRankingTable(List<Person> persons, Map<String, Double> scores, Map<String, Integer> frequencies, Map<String, Long> callDurations) {
        TableLayout table = dialog.findViewById(R.id.tableLayout);
        table.removeAllViews();

        TableRow header = new TableRow(getContext());
        addTableCell(header, "이름", true);
        addTableCell(header, "사진", true);
        addTableCell(header, "통화", true);
        addTableCell(header, "친밀도", true);
        table.addView(header);

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
        loadFaceImages();
        loadAllGalleryImages();
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
