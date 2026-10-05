package com.moneyteam.marketdata.provider.schwab.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts the refresh token at rest with AES-GCM.
 *
 * A refresh token is a long-lived credential: anyone holding it can mint
 * access tokens for the account for seven days. Storing it in plaintext would
 * mean a read-only database leak is equivalent to handing over the account, so
 * it is encrypted with a key that lives in the environment rather than the
 * database.
 *
 * GCM is used rather than CBC because it authenticates as well as encrypts -
 * tampering with the stored ciphertext fails loudly instead of decrypting to
 * garbage that then gets sent to the provider.
 *
 * A fresh random IV is generated per encryption and prefixed to the ciphertext.
 * Reusing an IV with GCM is catastrophic, so it is never derived or fixed.
 */
@Component
public class TokenCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;      // 96 bits, the GCM standard
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    /**
     * @param base64Key 32 bytes (256-bit), base64-encoded, supplied by the
     *                  environment. Startup fails rather than falling back to a
     *                  default, because a default key is the same as no key.
     */
    public TokenCipher(@Value("${app.marketdata.token-encryption-key}") String base64Key) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "app.marketdata.token-encryption-key must be base64-encoded", e);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "app.marketdata.token-encryption-key must decode to 32 bytes for AES-256; got "
                            + keyBytes.length);
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public byte[] encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv).put(ciphertext).array();
        } catch (Exception e) {
            // Never include the plaintext in the message.
            throw new IllegalStateException("Failed to encrypt token", e);
        }
    }

    public String decrypt(byte[] stored) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(stored);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt stored token", e);
        }
    }
}
