package com.pal.dipesh.razorpay.vault.service.impl;

import com.pal.dipesh.razorpay.common.entity.Money;
import com.pal.dipesh.razorpay.common.enums.CardBrand;
import com.pal.dipesh.razorpay.common.exception.ResourceNotFoundException;
import com.pal.dipesh.razorpay.common.util.RandomizerUtil;
import com.pal.dipesh.razorpay.payment.processor.PaymentProcessorRouter;
import com.pal.dipesh.razorpay.payment.processor.dto.PaymentProcessorRequest;
import com.pal.dipesh.razorpay.payment.processor.dto.PaymentProcessorResponse;
import com.pal.dipesh.razorpay.vault.config.VaultEncryptionConfig;
import com.pal.dipesh.razorpay.vault.dto.request.TokenizeRequest;
import com.pal.dipesh.razorpay.vault.dto.response.TokenizeResponse;
import com.pal.dipesh.razorpay.vault.entity.CardToken;
import com.pal.dipesh.razorpay.vault.entity.VaultCard;
import com.pal.dipesh.razorpay.vault.mapper.VaultCardMapper;
import com.pal.dipesh.razorpay.vault.repository.CardTokenRepository;
import com.pal.dipesh.razorpay.vault.repository.VaultCardRepository;
import com.pal.dipesh.razorpay.vault.service.VaultService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VaultServiceImpl implements VaultService {

    private final PaymentProcessorRouter paymentProcessorRouter;
    private final VaultCardRepository vaultCardRepository;
    private final CardTokenRepository cardTokenRepository;
    private final VaultCardMapper vaultCardMapper;
    private final BytesEncryptor dekEncryptor;

    @Override
    @Transactional
    public TokenizeResponse tokenize(TokenizeRequest tokenizeRequest, UUID merchantId) {

        String lastFour = tokenizeRequest.pan().substring(tokenizeRequest.pan().length() - 4);
        String bin = tokenizeRequest.pan().substring(0, 6);
        CardBrand cardBrand = detectBrand(tokenizeRequest.pan());

        byte[] dek = KeyGenerators.secureRandom(32).generateKey();
        byte[] encryptedPan = VaultEncryptionConfig.getEncryptor(dek).encrypt(tokenizeRequest.pan().getBytes(StandardCharsets.UTF_8));
        byte[] encryptedDek = dekEncryptor.encrypt(dek);

        VaultCard vaultCard = VaultCard.builder()
                .lastFour(lastFour)
                .bin(bin)
                .brand(cardBrand)
                .encryptedPan(encryptedPan)
                .encryptedDek(encryptedDek)
                .cardHolderName(tokenizeRequest.cardHolderName())
                .expiryMonth(tokenizeRequest.expiryMonth().toString())
                .expiryYear(tokenizeRequest.expiryYear().toString())
                .build();

        vaultCard = vaultCardRepository.save(vaultCard);
        String token = "tok_" + RandomizerUtil.randomBase64(32);

        cardTokenRepository.save(CardToken.builder()
                .token(token)
                .vaultCard(vaultCard)
                .customerId(tokenizeRequest.customerId())
                .merchantId(merchantId)
                .build()
        );

        return vaultCardMapper.toTokenizeResponse(vaultCard, token);
    }

    @Override
    public PaymentProcessorResponse charge(String token, UUID paymentId, Money amount, Map<String, Object> methodDetails) {
        CardToken cardToken = cardTokenRepository.findByTokenAndRevokedAtIsNull(token).orElseThrow(() -> new ResourceNotFoundException("CardToken", token));
        VaultCard vaultCard = cardToken.getVaultCard();

        byte[] dek = null;
        byte[] decryptedPanBytes = null;
        String decryptedPan = null;

        try {
            dek = dekEncryptor.decrypt(vaultCard.getEncryptedDek());
            decryptedPanBytes = VaultEncryptionConfig.getEncryptor(dek).decrypt(vaultCard.getEncryptedPan());

            decryptedPan = new String(decryptedPanBytes, StandardCharsets.UTF_8);
            String expiry = vaultCard.getExpiryMonth() + "/" + vaultCard.getExpiryYear();

            PaymentProcessorRequest paymentProcessorRequest = PaymentProcessorRequest.card(
                    paymentId,
                    amount,
                    decryptedPan,
                    expiry,
                    methodDetails
            );

            log.info("Vault charge registered, token={}*****", token.substring(0, 4));

            Arrays.fill(decryptedPanBytes, (byte) 0);

            return paymentProcessorRouter.charge(paymentProcessorRequest);
        } catch (Exception e) {
            log.warn("Vault charge failed, token={}*****", token.substring(0, 4));
            return new PaymentProcessorResponse.Failure("VAULT_CHARGE_FAILED", e.getMessage());
        } finally {
            // Clear sensitive data from memory
            if(decryptedPanBytes != null)
                Arrays.fill(decryptedPanBytes, (byte) 0);

            if(dek != null)
                Arrays.fill(dek, (byte) 0);

            decryptedPan = null;
        }
    }

    private CardBrand detectBrand(String pan) {
        if (pan.startsWith("4")) {
            return CardBrand.VISA;
        }

        if (pan.matches("^5[1-5].*")) {
            return CardBrand.MASTERCARD;
        }

        if (pan.matches("^3[47].*")) {
            return CardBrand.AMEX;
        }

        return CardBrand.RUPAY;
    }
}
