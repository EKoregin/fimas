package ru.korevg.fimas.dto.model;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelUpdateRequest {

        @Size(min = 1, max = 100, message = "Название от 1 до 100 символов")
        private String name;

        private Long vendorId;
}