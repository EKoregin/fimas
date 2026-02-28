package ru.korevg.fimas.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidPortRangeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPortRange {

    String message() default "Неверный формат порта. Ожидается: число 0-65535 или диапазон start-end где start ≤ end";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
