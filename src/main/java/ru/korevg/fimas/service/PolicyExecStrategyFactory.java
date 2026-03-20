package ru.korevg.fimas.service;

import org.springframework.stereotype.Component;
import ru.korevg.fimas.entity.Model;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PolicyExecStrategyFactory {

    private final Map<String, PolicyExecStrategy> strategiesByKey;

    public PolicyExecStrategyFactory(List<PolicyExecStrategy> strategies) {
        this.strategiesByKey = strategies.stream()
                .collect(Collectors.toMap(PolicyExecStrategy::getSupportedKey, Function.identity()));
    }

    public PolicyExecStrategy getStrategy(Model model) {
        String key = buildKey(model); // например "Cisco_ASA_5520"
        return strategiesByKey.get(key);
    }

    private String buildKey(Model model) {
        return model.getVendor().getName().toLowerCase();
    }
}
