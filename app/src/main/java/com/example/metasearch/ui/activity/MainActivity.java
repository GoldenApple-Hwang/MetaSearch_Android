package com.example.metasearch.ui.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.example.metasearch.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.metasearch.databinding.ActivityMainBinding;
import com.example.metasearch.interfaces.Update;
import com.example.metasearch.ui.fragment.HomeFragment;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_READ_PHOTOS = 101;
    private ActivityMainBinding binding;
    private int currentSelectedItemId;  // 현재 선택된 아이템 ID 저장

    private boolean hasPermission = false; // 권한 받은 이후 작동되는 코드에 쓰이는 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
        requestPermissions(); // 권한 요청
    }
    private void requestPermissions() {
        // Android 13(Tiramisu) 이상에서는 READ_MEDIA_IMAGES 권한 사용
        String storagePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        // 요청할 권한 목록에 알림 권한과 위치 정보 권한 추가
        String[] permissions = new String[] {
                storagePermission,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.POST_NOTIFICATIONS, // 알림 권한
                Manifest.permission.ACCESS_FINE_LOCATION, // 위치 정보 권한
                Manifest.permission.ACCESS_COARSE_LOCATION // 추가적으로 요청할 수 있습니다.
        };

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_READ_PHOTOS);
                return; // 권한이 허용되지 않은 경우 요청 후 종료
            }
        }
    }
    private void setupNavigation() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_search, R.id.navigation_graph
//                , R.id.navigation_like
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
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    if (permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE) || permissions[i].equals(Manifest.permission.READ_MEDIA_IMAGES)) {
                        loadAllImagesInHomeFragment();
                    } else if (permissions[i].equals(Manifest.permission.READ_CALL_LOG)) {

                    }
                } else {

                }
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