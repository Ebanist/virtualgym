package com.gymplanner.storage;

import com.gymplanner.common.persistence.BaseEntity;
import com.gymplanner.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "stored_files")
public class StoredFile extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String storageKey;

    private String thumbnailKey;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    protected StoredFile() {
    }

    public StoredFile(String storageKey, String thumbnailKey, String contentType, long sizeBytes, int width,
            int height, User uploadedBy) {
        this.storageKey = storageKey;
        this.thumbnailKey = thumbnailKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.uploadedBy = uploadedBy;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getThumbnailKey() {
        return thumbnailKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
