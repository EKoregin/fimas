package ru.korevg.fimas.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidInetValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidInet {
    String message() default "Некорректный формат IP-адреса или подсети";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}