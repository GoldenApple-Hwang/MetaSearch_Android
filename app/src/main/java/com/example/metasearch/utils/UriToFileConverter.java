package com.example.metasearch.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class UriToFileConverter {

    public static File getFileFromUri(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Unable to open input stream from URI");
        }

        File tempFile = createTemporaryFile(context, uri);
        FileOutputStream outputStream = new FileOutputStream(tempFile);

        try {
            byte[] buf = new byte[2048];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e("UriToFileConverter", "Error closing InputStream", e);
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                Log.e("UriToFileConverter", "Error closing FileOutputStream", e);
            }
        }

        return tempFile;
    }

    private static File createTemporaryFile(Context context, Uri uri) {
        // Use file extension if possible
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        fileExtension = fileExtension.isEmpty() ? "tmp" : "." + fileExtension;
        // Create a temporary file in the app's cache directory
        File tempFile = new File(context.getCacheDir(), "tempFile" + fileExtension);
        tempFile.deleteOnExit();
        return tempFile;
    }
}
