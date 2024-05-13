package com.example.metasearch.ui.activity;

import static android.content.ContentValues.TAG;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

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
        requestStoragePermission(); // 권한 요청
    }
    private void setupNavigation() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_search, R.id.navigation_graph, R.id.navigation_like
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
    // 권한 받은 이후 작동
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_PHOTOS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasPermission = true;

                // 권한이 허용된 경우 HomeFragment의 loadAllImages 호출
                loadAllImagesInHomeFragment();
            } else {
                // 권한이 거부된 경우, 사용자에게 권한이 필요한 이유를 설명하거나, 권한 없이 사용할 수 있는 기능으로 안내
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
    // 권한 요청 메서드
    private void requestStoragePermission() {
        Log.d(TAG,"저장소 권한 들어감");
        // 저장소 읽기 권한이 부여되지 않은 경우 권한 요청
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU){ //Android13 이상 버전의 경우
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, PERMISSIONS_REQUEST_READ_PHOTOS);
            }
        } // Android13 이하 버전의 경우
        else if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},PERMISSIONS_REQUEST_READ_PHOTOS);
        }

    }
}