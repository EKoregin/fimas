package ru.korevg.fimas.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ValueOfEnumValidator implements ConstraintValidator<ValueOfEnum, String> {

    private Class<? extends Enum<?>> enumClass;
    private String message;

    @Override
    public void initialize(ValueOfEnum constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        String upperValue = value.trim().toUpperCase();

        boolean valid = Arrays.stream(enumClass.getEnumConstants())
                .anyMatch(e -> e.name().equals(upperValue));

        if (!valid) {
            context.disableDefaultConstraintViolation();
            String allowed = Arrays.stream(enumClass.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            context.buildConstraintViolationWithTemplate(
                    message.replace("{enumClass.simpleName}", enumClass.getSimpleName()) +
                            ". Допустимые значения: " + allowed
            ).addConstraintViolation();
        }

        return valid;
    }
}