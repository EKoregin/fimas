package ru.korevg.fimas.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.korevg.fimas.dto.model.ModelCreateRequest;
import ru.korevg.fimas.dto.model.ModelResponse;
import ru.korevg.fimas.dto.model.ModelUpdateRequest;
import ru.korevg.fimas.entity.Model;
import ru.korevg.fimas.entity.Vendor;
import ru.korevg.fimas.exception.EntityExistsException;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.mapper.ModelMapper;
import ru.korevg.fimas.repository.ModelRepository;
import ru.korevg.fimas.repository.VendorRepository;
import ru.korevg.fimas.service.ModelService;
import ru.korevg.fimas.service.PolicyExecStrategyFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelServiceImpl implements ModelService {

    private final ModelRepository modelRepository;
    private final VendorRepository vendorRepository;
    private final ModelMapper modelMapper;
    private final PolicyExecStrategyFactory strategyFactory;

    @Override
    @Transactional
    public ModelResponse create(ModelCreateRequest request) {
        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new EntityNotFoundException("Vendor с ID " + request.getVendorId() + " не найден"));

        if (modelRepository.existsByNameAndVendorId(request.getName(), request.getVendorId())) {
            throw new EntityExistsException(
                    "Модель '" + request.getName() + "' уже существует у вендора " + vendor.getName()
            );
        }

        Model model = modelMapper.toEntity(request);
        model.setVendor(vendor);

        Model saved = modelRepository.save(model);
        return modelMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ModelResponse update(Long id, ModelUpdateRequest request) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Модель с ID " + id + " не найдена"));

        modelMapper.updateFromRequest(request, model);

        if (request.getVendorId() != null) {
            Vendor newVendor = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new EntityNotFoundException("Vendor с ID " + request.getVendorId() + " не найден"));
            model.setVendor(newVendor);
        }

        Model updated = modelRepository.save(model);
        return modelMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!modelRepository.existsById(id)) {
            throw new EntityNotFoundException("Модель с ID " + id + " не найдена");
        }
        modelRepository.deleteById(id);
    }

    @Override
    public Optional<ModelResponse> findById(Long id) {
        return modelRepository.findById(id)
                .map(modelMapper::toResponse);
    }

    @Override
    public Page<ModelResponse> findAll(Pageable pageable) {
        return modelRepository.findAll(pageable)
                .map(modelMapper::toResponse);
    }

    @Override
    public List<ModelResponse> findAll() {
        return modelRepository.findAll().stream()
                .map(modelMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Model getModelWithStrategy(Model model) {
        model.setStrategy(strategyFactory.getStrategy(model));
        return model;
    }

    @Override
    public long count() {
        return modelRepository.count();
    }
}