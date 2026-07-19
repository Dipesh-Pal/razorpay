package com.pal.dipesh.razorpay.vault.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class VaultEncryptionConfig {

    @Value("${app.vault.master-key}")
    private String masterKey;

    public static BytesEncryptor getEncryptor(byte[] dek) {
        SecretKey secretKey = new SecretKeySpec(dek, "AES");
        return new AesBytesEncryptor(secretKey, KeyGenerators.secureRandom(12), AesBytesEncryptor.CipherAlgorithm.GCM);
    }

    @Bean
    public BytesEncryptor dekEncryptor() {
        SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(masterKey), "AES");
        return new AesBytesEncryptor(secretKey, KeyGenerators.secureRandom(12), AesBytesEncryptor.CipherAlgorithm.GCM);
    }
}
