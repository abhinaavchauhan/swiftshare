package com.swiftshare.app.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for file operations: size formatting, name resolution, MIME type detection.
 */
public final class FileUtils {

    public interface SaveCallback {
        OutputStream getOutputStream(String fileName, long fileSize) throws IOException;
    }

    private FileUtils() {} // Prevent instantiation

    /**
     * Formats a file size in bytes into a human-readable string.
     */
    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", sizeInBytes / 1024.0);
        } else if (sizeInBytes < 1024L * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", sizeInBytes / (1024.0 * 1024));
        } else {
            return String.format(Locale.getDefault(), "%.2f GB", sizeInBytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Formats transfer speed (bytes/sec) into a human-readable string.
     */
    public static String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return bytesPerSecond + " B/s";
        } else if (bytesPerSecond < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB/s", bytesPerSecond / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.1f MB/s", bytesPerSecond / (1024.0 * 1024));
        }
    }

    /**
     * Formats remaining seconds into mm:ss or hh:mm:ss.
     */
    public static String formatETA(long seconds) {
        if (seconds < 0) return "—";
        if (seconds < 60) return String.format(Locale.getDefault(), "0:%02d", seconds);
        if (seconds < 3600) {
            return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60);
        }
        return String.format(Locale.getDefault(), "%d:%02d:%02d",
                seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    /**
     * Gets the display name of a file from its content URI.
     */
    public static String getFileName(Context context, Uri uri) {
        String fileName = null;

        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                // Fallback to path
            }
        }

        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }

        return fileName != null ? fileName : "unknown_file";
    }

    /**
     * Gets the file size from a content URI.
     */
    public static long getFileSize(Context context, Uri uri) {
        long size = 0;

        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex >= 0) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            } catch (Exception e) {
                // Fallback
            }
        }

        if (size == 0) {
            try {
                File file = new File(uri.getPath());
                size = file.length();
            } catch (Exception e) {
                // Ignore
            }
        }

        return size;
    }

    /**
     * Resolves the MIME type for a given URI.
     */
    public static String getMimeType(Context context, Uri uri) {
        String mimeType;

        if (uri.getScheme() != null && uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            mimeType = context.getContentResolver().getType(uri);
        } else {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    extension != null ? extension.toLowerCase(Locale.ROOT) : "");
        }

        return mimeType != null ? mimeType : "application/octet-stream";
    }

    /**
     * Formats a Unix timestamp into a human-readable date/time string.
     */
    public static String formatTimestamp(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        // Less than 24 hours
        if (diff < 86400000L) {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            long todayStart = now - (now % 86400000L);
            if (timestamp >= todayStart) {
                return "Today, " + sdf.format(new Date(timestamp));
            } else {
                return "Yesterday, " + sdf.format(new Date(timestamp));
            }
        }

        // Less than 7 days
        if (diff < 604800000L) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, h:mm a", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }

        // Default
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * Prepares a file in the public Downloads directory and returns an OutputStream.
     */
    public static OutputStream saveReceivedFile(Context context, String fileName, long fileSize) throws IOException {
        File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS);
        File swiftShareDir = new File(downloadsDir, "SwiftShare");
        if (!swiftShareDir.exists() && !swiftShareDir.mkdirs()) {
            throw new IOException("Could not create SwiftShare directory");
        }

        File saveFile = new File(swiftShareDir, fileName);
        
        // Handle duplicates
        if (saveFile.exists()) {
            String baseName = fileName;
            String extension = "";
            int lastDotIndex = fileName.lastIndexOf(".");
            if (lastDotIndex != -1) {
                baseName = fileName.substring(0, lastDotIndex);
                extension = fileName.substring(lastDotIndex);
            }
            int counter = 1;
            while (saveFile.exists()) {
                saveFile = new File(swiftShareDir, baseName + "_" + counter + extension);
                counter++;
            }
        }

        // Notify MediaScanner after closing the stream (handled by caller usually, but we store the path)
        final String finalPath = saveFile.getAbsolutePath();
        
        return new FileOutputStream(saveFile) {
            @Override
            public void close() throws IOException {
                super.close();
                // Trigger Media scan
                android.media.MediaScannerConnection.scanFile(context,
                        new String[]{finalPath},
                        null,
                        null);
            }
        };
    }
}
