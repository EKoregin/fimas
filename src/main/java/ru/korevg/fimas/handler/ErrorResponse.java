package ru.korevg.fimas.handler;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        int status,
        String error,
        Map<String, String> details,   // поле → сообщение
        LocalDateTime timestamp
) {}
