package com.rallymate.global.crypto;

import com.rallymate.global.exception.InternalServerErrorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import static com.rallymate.global.exception.ErrorCode.INTERNAL_SERVER_ERROR;

@Slf4j
@Component
public class PhoneCrypto {

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String HASH_ALGORITHM = "SHA-256";

    @Value("${crypto.aes-secret}")
    private String aesSecret;

    @Value("${crypto.hash-salt}")
    private String hashSalt;

    public String encrypt(String phoneNumber) {
        try {
            byte[] decodedSecret = Base64.getDecoder().decode(aesSecret);
            SecretKeySpec key = new SecretKeySpec(decodedSecret, "AES");
            IvParameterSpec iv = generateIv();

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);

            byte[] encrypted = cipher.doFinal(phoneNumber.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[16 + encrypted.length];
            System.arraycopy(iv.getIV(), 0, combined, 0, 16);
            System.arraycopy(encrypted, 0, combined, 16, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES 암호화 실패: {}", e.getMessage());
            throw new InternalServerErrorException(INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    public String decrypt(String encryptedPhoneNumber) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedPhoneNumber);

            byte[] ivBytes = new byte[16];
            byte[] encryptedBytes = new byte[combined.length - 16];
            System.arraycopy(combined, 0, ivBytes, 0, 16);
            System.arraycopy(combined, 16, encryptedBytes, 0, encryptedBytes.length);

            SecretKeySpec key = new SecretKeySpec(aesSecret.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec iv = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);

            return new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES 복호화 실패: {}", e.getMessage());
            throw new InternalServerErrorException(INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    public String hash(String phoneNumber) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            String salted = hashSalt + phoneNumber;
            byte[] hashed = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            log.error("Hash 실패: {}", e.getMessage());
            throw new InternalServerErrorException(INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    private IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }
}