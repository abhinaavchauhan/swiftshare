package com.swiftshare.app.data.model;

import android.net.Uri;

/**
 * Model representing a file selected for transfer.
 */
public class FileItem {

    private Uri uri;
    private String fileName;
    private String filePath;
    private long fileSize;
    private String mimeType;

    public FileItem() {}

    public FileItem(Uri uri, String fileName, long fileSize, String mimeType) {
        this.uri = uri;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    // Getters and Setters
    public Uri getUri() { return uri; }
    public void setUri(Uri uri) { this.uri = uri; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    /**
     * Returns a human-readable file size string.
     */
    public String getFormattedSize() {
        return com.swiftshare.app.utils.FileUtils.formatFileSize(fileSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileItem fileItem = (FileItem) o;
        return uri != null && uri.equals(fileItem.uri);
    }

    @Override
    public int hashCode() {
        return uri != null ? uri.hashCode() : 0;
    }
}
