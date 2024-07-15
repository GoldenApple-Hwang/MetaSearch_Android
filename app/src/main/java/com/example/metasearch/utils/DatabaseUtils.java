package com.example.metasearch.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class DatabaseUtils {
    private static final String PREFERENCES_FILE = "persistent_device_prefs";
    private static final String UNIQUE_ID_KEY = "unique_id";

    public static String getPersistentDeviceDatabaseName(Context context) {
        String uniqueId = getOrCreateUniqueId(context);
        return "db" + uniqueId;
    }

    private static String getOrCreateUniqueId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        String uniqueId = preferences.getString(UNIQUE_ID_KEY, null);

        if (uniqueId == null) {
            // UUID가 없으면 새로 생성하고 저장
            uniqueId = UUID.randomUUID().toString().replaceAll("-", "");
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(UNIQUE_ID_KEY, uniqueId);
            editor.apply();  // 또는 commit()
        }

        return uniqueId;
    }
}
