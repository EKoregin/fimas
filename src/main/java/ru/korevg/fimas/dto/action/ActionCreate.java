package ru.korevg.fimas.dto.action;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionCreate {

    @NotBlank(message = "Action name is required")
    private String name;

    @NotEmpty(message = "At least one command is required")
    private List<Long> commandIds;
}
