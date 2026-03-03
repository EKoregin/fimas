package ru.korevg.fimas.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPortRangeValidator implements ConstraintValidator<ValidPortRange, String> {

    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 65535;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // @NotBlank / @NotEmpty ставь отдельно, если нужно запретить пустоту
        }

        String trimmed = value.trim();

        // Вариант 1: просто одно число
        if (!trimmed.contains("-")) {
            return isValidPortNumber(trimmed, context);
        }

        // Вариант 2: диапазон start-end
        String[] parts = trimmed.split("-", 2);
        if (parts.length != 2) {
            return fail(context, "Ожидается формат число или start-end");
        }

        String startStr = parts[0].trim();
        String endStr = parts[1].trim();

        if (!isValidPortNumber(startStr, context) || !isValidPortNumber(endStr, context)) {
            return false;
        }

        try {
            int start = Integer.parseInt(startStr);
            int end = Integer.parseInt(endStr);

            if (start > end) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Начальный порт не может быть больше конечного")
                        .addConstraintViolation();
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            return fail(context, "Порты должны быть числами");
        }
    }

    private boolean isValidPortNumber(String s, ConstraintValidatorContext ctx) {
        try {
            int port = Integer.parseInt(s);
            if (port < MIN_PORT || port > MAX_PORT) {
                ctx.disableDefaultConstraintViolation();
                ctx.buildConstraintViolationWithTemplate("Порт должен быть от 0 до 65535")
                        .addConstraintViolation();
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return fail(ctx, "Ожидается целое число");
        }
    }

    private boolean fail(ConstraintValidatorContext ctx, String customMessage) {
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(customMessage)
                .addConstraintViolation();
        return false;
    }
}