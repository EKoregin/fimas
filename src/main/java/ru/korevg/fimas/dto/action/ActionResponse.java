package ru.korevg.fimas.dto.action;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.korevg.fimas.dto.command.CommandResponse;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionResponse {

    private Long id;
    private String name;

    @Builder.Default
    private Set<CommandResponse> commands = new LinkedHashSet<>();
}
