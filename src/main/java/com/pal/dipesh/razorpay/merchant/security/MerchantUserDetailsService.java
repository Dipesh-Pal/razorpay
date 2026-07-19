package com.pal.dipesh.razorpay.merchant.security;

import com.pal.dipesh.razorpay.common.exception.ResourceNotFoundException;
import com.pal.dipesh.razorpay.merchant.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        return appUserRepository.findByEmail(username).orElseThrow(() -> new ResourceNotFoundException("AppUser", username));
    }
}
