package com.pal.dipesh.razorpay;

import com.pal.dipesh.razorpay.merchant.security.CookieProperties;
import com.pal.dipesh.razorpay.merchant.security.rsa.RsaKeyProperties;
import com.pal.dipesh.razorpay.payment.simulator.SimulatorConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableScheduling
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
@EnableConfigurationProperties({SimulatorConfig.class, RsaKeyProperties.class, CookieProperties.class})
public class RazorpayApplication {

	public static void main(String[] args) {
		SpringApplication.run(RazorpayApplication.class, args);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(BCryptPasswordEncoder.BCryptVersion.$2B, 12);
	}
}