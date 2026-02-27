package ru.korevg.fimas.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.korevg.fimas.dto.policy.PolicyCreateRequest;
import ru.korevg.fimas.dto.policy.PolicyResponse;
import ru.korevg.fimas.dto.policy.PolicyUpdateRequest;

public interface PolicyService {

    PolicyResponse create(PolicyCreateRequest request);

    PolicyResponse update(Long id, PolicyUpdateRequest request);

    void delete(Long id);

    PolicyResponse findById(Long id);

    Page<PolicyResponse> findAll(Pageable pageable);

    Page<PolicyResponse> findByFirewallId(Long firewallId, Pageable pageable);
}
