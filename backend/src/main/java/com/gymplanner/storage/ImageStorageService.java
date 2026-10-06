package com.gymplanner.storage;

import com.gymplanner.common.error.ApiException;
import com.gymplanner.common.error.NotFoundException;
import com.gymplanner.user.User;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Walidacja, zapis i generowanie miniatur zdjęć (jpg/png/webp). */
@Service
public class ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);
    static final String THUMBNAIL_CONTENT_TYPE = "image/jpeg";

    private final FileStorage storage;
    private final StoredFileRepository files;
    private final StorageProperties properties;

    public ImageStorageService(FileStorage storage, StoredFileRepository files, StorageProperties properties) {
        this.storage = storage;
        this.files = files;
        this.properties = properties;
    }

    @Transactional
    public StoredFile storeImage(MultipartFile upload, User uploadedBy) {
        byte[] bytes = readBytes(upload);
        if (bytes.length == 0) {
            throw invalid("file_empty", "File is empty");
        }
        if (properties.maxFileSize() != null && bytes.length > properties.maxFileSize().toBytes()) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "file_too_large", "File is too large");
        }
        ImageType type = ImageType.detect(bytes)
                .orElseThrow(() -> invalid("unsupported_file_type", "Only JPG, PNG and WebP images are allowed"));
        BufferedImage image = decode(bytes);

        String baseKey = "images/" + UUID.randomUUID();
        String key = baseKey + "." + type.extension();
        String thumbnailKey = baseKey + "_thumb.jpg";
        try {
            storage.store(key, bytes, type.contentType());
            storage.store(thumbnailKey, thumbnail(image), THUMBNAIL_CONTENT_TYPE);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store image", e);
        }
        return files.save(new StoredFile(key, thumbnailKey, type.contentType(), bytes.length, image.getWidth(),
                image.getHeight(), uploadedBy));
    }

    @Transactional(readOnly = true)
    public StoredFile get(UUID id) {
        return files.findById(id).orElseThrow(() -> new NotFoundException("File"));
    }

    public InputStream open(String key) {
        try {
            return storage.load(key);
        } catch (IOException e) {
            log.warn("Stored file {} is missing", key);
            throw new NotFoundException("File");
        }
    }

    private BufferedImage decode(byte[] bytes) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw invalid("unsupported_file_type", "Unsupported image");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                // Ochrona przed „bombami dekompresyjnymi” – sprawdzamy wymiary przed dekodowaniem.
                int max = properties.maxImageDimension();
                if (reader.getWidth(0) > max || reader.getHeight(0) > max) {
                    throw invalid("image_too_large", "Image dimensions are too large");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw invalid("invalid_image", "File is not a valid image");
        }
    }

    private byte[] thumbnail(BufferedImage source) throws IOException {
        // Spłaszczamy przezroczystość na białe tło – miniatura jest w JPEG.
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int size = properties.thumbnailSize();
        Thumbnails.of(rgb).size(size, size).outputFormat("jpg").outputQuality(0.8).toOutputStream(out);
        return out.toByteArray();
    }

    private static byte[] readBytes(MultipartFile upload) {
        try {
            return upload.getBytes();
        } catch (IOException e) {
            throw invalid("invalid_image", "Could not read file");
        }
    }

    private static ApiException invalid(String code, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
