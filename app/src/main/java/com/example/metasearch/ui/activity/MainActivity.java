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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.metasearch.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_READ_PHOTOS = 101;
    private ActivityMainBinding binding;

    private boolean hasPermission = false; // 권한 받은 이후 작동되는 코드에 쓰이는 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        AppBarConfiguration appBarConfiguration =
                new AppBarConfiguration.Builder(
                        R.id.navigation_home,
                        R.id.navigation_search,
                        R.id.navigation_composite,
                        R.id.navigation_graph
                        ).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        requestStoragePermission(); // 권한 요청
    }

    // 권한 받은 이후 작동
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_PHOTOS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용된 경우, 필요한 작업 수행
                // 예: 이미지 업로드 작업 시작
                //이미지 분석 처리 코드
                hasPermission = true;

            } else {
                // 권한이 거부된 경우, 사용자에게 권한이 필요한 이유를 설명하거나, 권한 없이 사용할 수 있는 기능으로 안내
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