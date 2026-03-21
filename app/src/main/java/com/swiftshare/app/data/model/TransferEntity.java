package com.swiftshare.app.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity representing a file transfer record.
 * Stores metadata about each transfer for the history screen.
 */
@Entity(tableName = "transfers")
public class TransferEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String fileName;
    private String filePath;
    private long fileSize;
    private String mimeType;
    private String deviceName;
    private String deviceAddress;
    private String direction; // "SENT" or "RECEIVED"
    private String status;    // "PENDING", "IN_PROGRESS", "COMPLETED", "FAILED", "CANCELLED"
    private int progress;     // 0-100
    private long transferSpeed; // bytes per second
    private long startTime;
    private long endTime;
    private long timestamp;

    // Default constructor
    public TransferEntity() {
        this.timestamp = System.currentTimeMillis();
        this.status = "PENDING";
        this.progress = 0;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getDeviceAddress() { return deviceAddress; }
    public void setDeviceAddress(String deviceAddress) { this.deviceAddress = deviceAddress; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public long getTransferSpeed() { return transferSpeed; }
    public void setTransferSpeed(long transferSpeed) { this.transferSpeed = transferSpeed; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    /**
     * Returns whether this was an outgoing (sent) transfer.
     */
    public boolean isSent() {
        return "SENT".equals(direction);
    }

    /**
     * Returns whether the transfer completed successfully.
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    /**
     * Returns whether the transfer is still in progress.
     */
    public boolean isInProgress() {
        return "IN_PROGRESS".equals(status);
    }

    /**
     * Returns the transfer duration in milliseconds.
     */
    public long getDuration() {
        if (endTime > 0 && startTime > 0) {
            return endTime - startTime;
        }
        return 0;
    }
}
