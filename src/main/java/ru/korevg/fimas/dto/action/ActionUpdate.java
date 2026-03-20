package ru.korevg.fimas.dto.action;

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
public class ActionUpdate {

    private String name;

    @Builder.Default
    private Set<Long> commandIds = new LinkedHashSet<>();
}
