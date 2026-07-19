package com.pal.dipesh.razorpay.vault.mapper;

import com.pal.dipesh.razorpay.vault.dto.response.TokenizeResponse;
import com.pal.dipesh.razorpay.vault.entity.VaultCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VaultCardMapper {
    VaultCardMapper INSTANCE = Mappers.getMapper(VaultCardMapper.class);

    @Mappings({
            @Mapping(target = "token", source = "token"),
            @Mapping(target = "lastFour", source = "vaultCard.lastFour"),
            @Mapping(target = "brand", source = "vaultCard.brand"),
            @Mapping(target = "expiryMonth", source = "vaultCard.expiryMonth"),
            @Mapping(target = "expiryYear", source = "vaultCard.expiryYear")
    })
    TokenizeResponse toTokenizeResponse(VaultCard vaultCard, String token);
}
