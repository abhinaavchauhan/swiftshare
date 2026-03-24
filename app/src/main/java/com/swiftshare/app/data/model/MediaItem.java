package com.swiftshare.app.data.model;

import android.net.Uri;
import java.io.Serializable;
import java.util.Objects;

/**
 * Model representing a media item (Photo, Video, Music, App, File) in the picker.
 */
public class MediaItem implements Serializable {
    public enum Type { PHOTO, VIDEO, MUSIC, APP, FILE }

    private final long id;
    private final String name;
    private final long size;
    private final long dateAdded;
    private final Uri uri;
    private final Type type;
    private final String duration; // For video/audio
    private boolean isSelected;

    public MediaItem(long id, String name, long size, long dateAdded, Uri uri, Type type, String duration) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.dateAdded = dateAdded;
        this.uri = uri;
        this.type = type;
        this.duration = duration;
        this.isSelected = false;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public long getSize() { return size; }
    public long getDateAdded() { return dateAdded; }
    public Uri getUri() { return uri; }
    public Type getType() { return type; }
    public String getDuration() { return duration; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaItem mediaItem = (MediaItem) o;
        return id == mediaItem.id && type == mediaItem.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }
}
