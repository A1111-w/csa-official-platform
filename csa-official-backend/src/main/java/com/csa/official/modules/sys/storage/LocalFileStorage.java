package com.csa.official.modules.sys.storage;

import com.csa.official.common.exception.CsaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
public class LocalFileStorage implements FileStorage {

    private final Path baseDirectory;

    public LocalFileStorage(@Value("${csa.upload-path}") String basePath) {
        this.baseDirectory = Paths.get(basePath).toAbsolutePath().normalize();
    }

    @Override
    public String provider() {
        return "LOCAL";
    }

    @Override
    public void store(String storageKey, InputStream input) throws IOException {
        Path destination = resolve(storageKey);
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), ".upload-", ".tmp");
        try {
            Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public Path resolve(String storageKey) {
        if (!StringUtils.hasText(storageKey) || !storageKey.startsWith("/files/")) {
            throw new CsaException(400, "非法文件存储键");
        }
        String relative = storageKey.substring("/files/".length());
        Path resolved = baseDirectory.resolve(relative).normalize();
        if (!resolved.startsWith(baseDirectory) || relative.contains("..")) {
            throw new CsaException(400, "非法文件存储键");
        }
        return resolved;
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }
}
