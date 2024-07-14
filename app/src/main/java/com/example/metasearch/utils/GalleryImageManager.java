package com.example.metasearch.utils;

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
    //갤러리에서 모든 이미지의 실제 경로를 가져오는 메소드
    public static ArrayList<String> getAllGalleryImagesUriToString(Context context) {
        ArrayList<String> imagePaths = new ArrayList<>();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                imagePaths.add(imagePath);
            }
            cursor.close();
        }
        return imagePaths;
    }
    public static List<Uri> getAllGalleryImagesUri(Context context) {
        List<Uri> imageUris = new ArrayList<>();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        // MIME 타입을 필터링하여 JPEG 및 PNG 이미지만 조회
        String[] projection = {MediaStore.Images.Media._ID};
        String selection = MediaStore.Images.Media.MIME_TYPE + "=? OR " + MediaStore.Images.Media.MIME_TYPE + "=?";
        String[] selectionArgs = new String[] {"image/jpeg", "image/png"};

        Cursor cursor = context.getContentResolver().query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
        );
        if (cursor != null) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                imageUris.add(imageUri);
            }
            cursor.close();
        }
        return imageUris;
    }
    // 갤러리에서 모든 이미지의 URI를 가져오는 메서드
//    public static List<Uri> getAllGalleryImagesUri(Context context) {
//        List<Uri> imageUris = new ArrayList<>();
//        String[] projection = {MediaStore.Images.Media._ID};
//        Cursor cursor = context.getContentResolver().query(
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                projection,
//                null,
//                null,
//                null
//        );
//        if (cursor != null) {
//            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
//            while (cursor.moveToNext()) {
//                long id = cursor.getLong(idColumn);
//                Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
//                imageUris.add(uri);
//            }
//            cursor.close();
//        }
//        return imageUris;
//    }
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

    // Neo4j 서버에서 받은 이름과 갤러리의 이름을 비교하여 일치하는 URI만 반환하는 메서드
    public static Uri findMatchedUri(String photoNameFromServer, Context context) {
        Map<String, Uri> allGalleryUrisWithName = getAllGalleryImagesUriWithName(context);

        // photoNameFromServer가 Map의 키로 존재하는지 확인하고 해당 URI 반환
        if (allGalleryUrisWithName.containsKey(photoNameFromServer)) {
            return allGalleryUrisWithName.get(photoNameFromServer);
        }
        // 일치하는 사진이 없을 경우 null 반환
        return null;
    }
    // 갤러리에서 URI에 해당하는 파일 이름을 찾는 메서드
    public static String getFileNameFromUri(Context context, Uri imageUri) {
        String fileName = null;
        String[] projection = {MediaStore.Images.Media.DISPLAY_NAME};
        try (Cursor cursor = context.getContentResolver().query(imageUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                fileName = cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileName;
    }
}
