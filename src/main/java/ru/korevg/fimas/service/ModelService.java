package ru.korevg.fimas.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.korevg.fimas.dto.model.ModelCreateRequest;
import ru.korevg.fimas.dto.model.ModelResponse;
import ru.korevg.fimas.dto.model.ModelUpdateRequest;

import java.util.List;
import java.util.Optional;

public interface ModelService {

    ModelResponse create(ModelCreateRequest request);

    ModelResponse update(Long id, ModelUpdateRequest request);

    void delete(Long id);

    Optional<ModelResponse> findById(Long id);

    Page<ModelResponse> findAll(Pageable pageable);

    List<ModelResponse> findAll();
}
