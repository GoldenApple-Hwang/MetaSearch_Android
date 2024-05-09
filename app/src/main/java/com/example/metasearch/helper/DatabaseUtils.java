package com.example.metasearch.helper;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class DatabaseUtils {
    private static final String UNIQUE_ID_FILE = "unique_id.txt";
    public static String getPersistentDeviceDatabaseName(Context context) {
        String uniqueId = getOrCreateUniqueId(context);
        return "db_" + uniqueId;
    }
    private static String getOrCreateUniqueId(Context context) {
        File file = new File(context.getFilesDir(), UNIQUE_ID_FILE);
        String uniqueId = null;

        // 고유 식별자가 이미 저장되어 있는지 확인
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[36]; // UUID 길이
                int bytesRead = fis.read(buffer);
                if (bytesRead == 36) {
                    uniqueId = new String(buffer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 파일에 고유 식별자가 없다면 새로 생성하고 저장
        if (uniqueId == null) {
            uniqueId = UUID.randomUUID().toString().replaceAll("-", "");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(uniqueId.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return uniqueId;
    }
}