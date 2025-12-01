package com.salesmaster.salesmasterpro.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidNombreValidator implements ConstraintValidator<ValidNombre, String> {
    
    // Expresión regular: letras, espacios, acentos, ñ, guiones, apóstrofes
    // Mínimo 2 caracteres, máximo 80
    private static final String NOMBRE_PATTERN = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]{2,80}$";

    @Override
    public void initialize(ValidNombre constraintAnnotation) {
    }

    @Override
    public boolean isValid(String nombre, ConstraintValidatorContext context) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return false;
        }
        
        // Verificar que no sea solo espacios
        if (nombre.trim().length() < 2) {
            return false;
        }
        
        return nombre.matches(NOMBRE_PATTERN);
    }
}

