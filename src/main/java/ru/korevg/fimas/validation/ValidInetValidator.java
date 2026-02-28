package ru.korevg.fimas.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ValidInetValidator implements ConstraintValidator<ValidInet, String> {

    @Autowired
    private InetValidator inetValidator;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // @NotNull отдельно
        }
        return inetValidator.isValidInet(value);
    }
}