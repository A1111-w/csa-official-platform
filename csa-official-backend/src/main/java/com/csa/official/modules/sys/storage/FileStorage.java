package com.csa.official.modules.sys.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * File storage boundary. The current provider is local volume storage; an S3-compatible
 * implementation can replace it without changing upload metadata or authorization code.
 */
public interface FileStorage {

    String provider();

    void store(String storageKey, InputStream input) throws IOException;

    Path resolve(String storageKey);

    void delete(String storageKey) throws IOException;
}
