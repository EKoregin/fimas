package ru.korevg.fimas.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.korevg.fimas.dto.firewall.FirewallCreateRequest;
import ru.korevg.fimas.dto.firewall.FirewallResponse;
import ru.korevg.fimas.dto.firewall.FirewallUpdateRequest;

public interface FirewallService {

    FirewallResponse create(FirewallCreateRequest request);

    FirewallResponse update(Long id, FirewallUpdateRequest request);

    void delete(Long id);

    FirewallResponse findById(Long id);

    Page<FirewallResponse> findAll(Pageable pageable);
}
