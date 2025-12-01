package com.salesmaster.salesmasterpro.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.validator.routines.EmailValidator;

public class ValidEmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final EmailValidator commonsEmailValidator = EmailValidator.getInstance(true, true);

    @Override
    public void initialize(ValidEmail constraintAnnotation) {
        // No se requiere inicialización
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.trim().isEmpty()) {
            return true; // La validación de @NotBlank se encarga de esto
        }
        
        // Valida el formato del email usando Apache Commons Validator
        // getInstance(true, true): permite verificar dominios válidos y formato RFC 5322
        return commonsEmailValidator.isValid(email);
    }
}

