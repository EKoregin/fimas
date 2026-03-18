package ru.korevg.fimas.dto.command;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.korevg.fimas.entity.CommandType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandCreate {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Command text is required")
    private String command;

    @NotNull(message = "Command type is required")
    private CommandType commandType;

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;
}
