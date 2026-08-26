package com.csa.official.modules.sys.service;

import com.csa.official.common.cache.KeyValueStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MailRecoveryStore {

    private static final String KEY_PREFIX = "mail:recovery:";

    private final KeyValueStore keyValueStore;
    private final ObjectMapper objectMapper;
    private final long ttlSeconds;

    public MailRecoveryStore(KeyValueStore keyValueStore,
                             ObjectMapper objectMapper,
                             @Value("${csa.mail.recovery-ttl-seconds:360}") long ttlSeconds) {
        this.keyValueStore = keyValueStore;
        this.objectMapper = objectMapper;
        this.ttlSeconds = Math.max(60, Math.min(ttlSeconds, 900));
    }

    public void save(Long deliveryId, String recipient, String messageType,
                     String codeKey, String limitKey, String code) {
        if (deliveryId == null) {
            throw new IllegalStateException("Mail delivery id is required for recovery");
        }
        RecoveryPayload payload = new RecoveryPayload(
                recipient, messageType, codeKey, limitKey, sha256(code));
        try {
            keyValueStore.setString(
                    recoveryKey(deliveryId),
                    objectMapper.writeValueAsString(payload),
                    ttlSeconds,
                    TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Mail recovery payload could not be serialized", e);
        }
    }

    public Optional<RecoveryPayload> find(Long deliveryId) {
        if (deliveryId == null) {
            return Optional.empty();
        }
        String encoded = keyValueStore.getString(recoveryKey(deliveryId));
        if (encoded == null || encoded.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(encoded, RecoveryPayload.class));
        } catch (JsonProcessingException e) {
            log.warn("Discarding invalid mail recovery payload: deliveryId={}", deliveryId);
            delete(deliveryId);
            return Optional.empty();
        }
    }

    public boolean matchesCode(RecoveryPayload payload, String code) {
        if (payload == null || code == null || payload.codeHash() == null) {
            return false;
        }
        return MessageDigest.isEqual(
                payload.codeHash().getBytes(StandardCharsets.US_ASCII),
                sha256(code).getBytes(StandardCharsets.US_ASCII));
    }

    public boolean matchesDelivery(RecoveryPayload payload, String recipientHash, String messageType) {
        if (payload == null || recipientHash == null || messageType == null) {
            return false;
        }
        return MessageDigest.isEqual(
                recipientHash.getBytes(StandardCharsets.US_ASCII),
                sha256(payload.recipient()).getBytes(StandardCharsets.US_ASCII))
                && messageType.equals(payload.messageType());
    }

    public void delete(Long deliveryId) {
        if (deliveryId != null) {
            keyValueStore.delete(recoveryKey(deliveryId));
        }
    }

    static String recoveryKey(Long deliveryId) {
        return KEY_PREFIX + deliveryId;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public record RecoveryPayload(
            String recipient,
            String messageType,
            String codeKey,
            String limitKey,
            String codeHash) {
    }
}
