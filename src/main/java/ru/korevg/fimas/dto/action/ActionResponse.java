package ru.korevg.fimas.dto.action;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.korevg.fimas.dto.command.CommandResponse;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionResponse {

    private Long id;
    private String name;

    @Builder.Default
    private List<CommandResponse> commands = new ArrayList<>();
}
