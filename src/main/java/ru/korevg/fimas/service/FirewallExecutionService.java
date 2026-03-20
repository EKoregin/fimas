package ru.korevg.fimas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.korevg.fimas.dto.action.ActionResponse;
import ru.korevg.fimas.entity.Model;
import ru.korevg.fimas.mapper.ActionCommandMapper;
import ru.korevg.fimas.repository.ModelRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FirewallExecutionService {

    private final ActionCommandService actionCommandService;
    private final ModelService modelService;
    private final ActionCommandMapper actionCommandMapper;

    public List<String> executeActionOnModel(Model model, Long actionId, String host, String username, String password)
            throws Exception {

        model = modelService.getModelWithStrategy(model);

        ActionResponse actionDto = actionCommandService.getActionById(actionId)
                .orElseThrow(() -> new RuntimeException("Action not found"));

        return model.getStrategy().execute(actionCommandMapper.toEntity(actionDto), host, 22, username, password);
    }
}
