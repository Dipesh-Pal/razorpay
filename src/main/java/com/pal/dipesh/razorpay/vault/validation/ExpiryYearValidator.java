package com.pal.dipesh.razorpay.vault.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Year;

public class ExpiryYearValidator implements ConstraintValidator<ExpiryYear, Integer> {

    @Override
    public boolean isValid(Integer inputYear, ConstraintValidatorContext context) {
        if (inputYear == null) {
            return false;
        }

        int currentYear = Year.now().getValue();
        return inputYear >= currentYear && inputYear <= currentYear + 20;
    }
}
