package com.pal.dipesh.razorpay.vault.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Configuration
public class VaultEncryptionConfig {

    public static BytesEncryptor getEncryptor(byte[] dek) {
        SecretKey secretKey = new SecretKeySpec(dek, "AES");
        return new AesBytesEncryptor(secretKey, KeyGenerators.secureRandom(12), AesBytesEncryptor.CipherAlgorithm.GCM);
    }
}
