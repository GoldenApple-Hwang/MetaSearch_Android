package com.example.metasearch.manager;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GalleryImageManager {

    // 갤러리에서 모든 이미지의 URI를 가져오는 메서드
    public static List<Uri> getAllGalleryImagesUri(Context context) {
        List<Uri> imageUris = new ArrayList<>();
        String[] projection = {MediaStore.Images.Media._ID};
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null
        );
        if (cursor != null) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                imageUris.add(uri);
            }
            cursor.close();
        }
        return imageUris;
    }
    // 갤러리에서 모든 이미지의 URI와 파일 이름을 매핑하여 가져오는 메서드
    public static Map<String, Uri> getAllGalleryImagesUriWithName(Context context) {
        Map<String, Uri> images = new HashMap<>();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                images.put(name, contentUri);
            }
        }
        return images;
    }
    // 갤러리에서 모든 이미지의 이름을 확장자 없이 가져오는 메서드
    public static List<String> getAllImageNamesWithoutExtension(Context context) {
        List<String> imageNamesWithoutExtension = new ArrayList<>();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DISPLAY_NAME};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            while (true) {
                assert cursor != null;
                if (!cursor.moveToNext()) break;
                @SuppressLint("Range") String imageNameWithExtension = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
                String imageNameWithoutExtension = imageNameWithExtension.substring(0, imageNameWithExtension.lastIndexOf('.'));
                imageNamesWithoutExtension.add(imageNameWithoutExtension);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageNamesWithoutExtension;
    }
    // Neo4j 서버에서 받은 이름과 갤러리의 이름을 비교하여 일치하는 URI만 리스트로 반환하는 메서드
    public static List<Uri> findMatchedUris(List<String> photoNamesFromServer, Context context) {
        List<Uri> matchedUris = new ArrayList<>();
        Map<String, Uri> allGalleryUrisWithName = getAllGalleryImagesUriWithName(context);

        for (String photoName : photoNamesFromServer) {
            if (allGalleryUrisWithName.containsKey(photoName)) {
                Uri matchedUri = allGalleryUrisWithName.get(photoName);
                if (matchedUri != null) {
                    matchedUris.add(matchedUri);
                }
            }
        }
        return matchedUris;
    }
}
