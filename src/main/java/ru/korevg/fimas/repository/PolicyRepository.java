package ru.korevg.fimas.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.korevg.fimas.entity.Policy;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByNameAndFirewallId(String name, Long firewallId);

    boolean existsByNameAndFirewallId(String name, Long firewallId);

    List<Policy> findByFirewallId(Long firewallId);

    Page<Policy> findByFirewallId(Long firewallId, Pageable pageable);

    long countByFirewallId(Long firewallId);
}
