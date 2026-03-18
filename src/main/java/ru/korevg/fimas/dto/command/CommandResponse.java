package ru.korevg.fimas.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.korevg.fimas.entity.CommandType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandResponse {
    private Long id;
    private String name;
    private String command;
    private CommandType commandType;
    private Long vendorId;
    private String vendorName;
}
