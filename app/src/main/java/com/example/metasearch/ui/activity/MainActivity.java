package com.example.metasearch.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.example.metasearch.R;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.metasearch.databinding.ActivityMainBinding;
import com.example.metasearch.network.interfaces.Update;
import com.example.metasearch.ui.fragment.HomeFragment;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_READ_PHOTOS = 101;
    private ActivityMainBinding binding;
    private int currentSelectedItemId;  // 현재 선택된 아이템 ID 저장

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    allGranted &= granted;
                }
                if (allGranted) {
                    loadAllImagesInHomeFragment();
                } else {
                    showPermissionDeniedDialog();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
        checkAndRequestPermissions(); // 권한 요청
    }

    private void checkAndRequestPermissions() {
        // Android 13(Tiramisu) 이상에서는 READ_MEDIA_IMAGES 권한 사용
        String storagePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        // 요청할 권한 목록에 알림 권한과 위치 정보 권한 추가
        String[] allPermissions = new String[] {
                storagePermission,
                Manifest.permission.ACCESS_FINE_LOCATION, // 위치 정보 권한
                Manifest.permission.ACCESS_COARSE_LOCATION, // 추가적으로 요청 가능
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG,
                Manifest.permission.POST_NOTIFICATIONS // 알림 권한
        };

        String[] essentialPermissions = new String[] {
                storagePermission
        };

        if (isFirstRun()) {
            requestPermissionLauncher.launch(allPermissions);
        } else if (!isPermissionGranted(storagePermission)) {
            requestPermissionLauncher.launch(essentialPermissions);
        } else {
            loadAllImagesInHomeFragment();
        }
    }

    private boolean isFirstRun() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("isFirstRun", true);
        if (isFirstRun) {
            prefs.edit().putBoolean("isFirstRun", false).apply();
        }
        return isFirstRun;
    }

    private boolean isPermissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void showPermissionDeniedDialog() {
        if (!isPermissionGranted(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
                    .setTitle("권한 요청")
                    .setMessage("사진 권한이 필요합니다. 앱 설정에서 권한을 허용해주세요.")
                    .setPositiveButton("설정으로 이동", (dialog, which) -> openAppSettings())
                    .setNegativeButton("취소", null)
                    .show();
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void setupNavigation() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_person, R.id.navigation_home, R.id.navigation_search, R.id.navigation_graph
        ).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // 최초 선택 아이템 설정
        currentSelectedItemId = binding.navView.getSelectedItemId();

        binding.navView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.refresh) {
                refreshData();
                // 'refresh' 선택 시 시각적 변화 없이 기능만 실행, 선택 상태 변화 방지
                return false;  // false를 반환하여 아이템 선택 상태를 변경하지 않음
            }
            // 나머지 아이템 선택 시 정상적인 네비게이션 처리
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) {
                currentSelectedItemId = item.getItemId();  // 선택된 아이템 ID 업데이트
            }
            return handled;
        });
    }

    private void refreshData() {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        if (navHostFragment != null) {
            Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
            if (currentFragment instanceof Update) {
                ((Update) currentFragment).performDataUpdate();
            }
        }
    }

    // 하단의 네비 바 숨김
    public void hideBottomNavigationView() {
        binding.navView.animate().translationY(binding.navView.getHeight());
    }

    // 하단의 네비 바 보임
    public void showBottomNavigationView() {
        binding.navView.animate().translationY(0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_PHOTOS && grantResults.length > 0) {
            boolean allGranted = true;
            for (int grantResult : grantResults) {
                allGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
            }
            if (allGranted) {
                loadAllImagesInHomeFragment();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    // HomeFragment에 접근하여 loadAllImages 호출
    private void loadAllImagesInHomeFragment() {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        if (navHostFragment != null) {
            Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
            if (currentFragment instanceof HomeFragment) {
                ((HomeFragment) currentFragment).loadAllGalleryImages();
            }
        }
    }
}
