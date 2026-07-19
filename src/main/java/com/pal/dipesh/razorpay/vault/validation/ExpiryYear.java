package com.pal.dipesh.razorpay.vault.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = ExpiryYearValidator.class)
public @interface ExpiryYear {

    String message() default "Expiry Year cannot be in past and cannot be more than 20 years in future";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
