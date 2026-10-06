package com.gymplanner.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstrakcja magazynu plików. MVP: dysk lokalny ({@link LocalFileStorage});
 * docelowo implementacja S3/MinIO bez zmian w logice aplikacji.
 */
public interface FileStorage {

    void store(String key, byte[] content, String contentType) throws IOException;

    InputStream load(String key) throws IOException;

    void delete(String key) throws IOException;
}
