package ru.korevg.fimas.dto.action;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionCreate {

    @NotBlank(message = "Action name is required")
    private String name;

    @Builder.Default
    @NotEmpty(message = "At least one command is required")
    private Set<Long> commandIds = new LinkedHashSet<>();

    @NotNull(message = "Model ID is required")
    private Long modelId;
}
