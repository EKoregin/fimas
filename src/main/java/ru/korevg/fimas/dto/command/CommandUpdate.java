package ru.korevg.fimas.dto.command;

import ru.korevg.fimas.entity.CommandType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandUpdate {

    private String name;
    private String command;
    private CommandType commandType;
}
