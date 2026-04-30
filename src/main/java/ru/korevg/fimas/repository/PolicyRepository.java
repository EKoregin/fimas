package ru.korevg.fimas.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.korevg.fimas.entity.Firewall;
import ru.korevg.fimas.entity.Policy;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByNameAndFirewallId(String name, Long firewallId);

    boolean existsByNameAndFirewallId(String name, Long firewallId);

//    List<Policy> findByFirewallId(Long firewallId);

    @EntityGraph(attributePaths = {"srcZone", "dstZone", "srcAddresses", "dstAddresses", "services"})
    List<Policy> findByFirewallId(Long firewallId);

    Page<Policy> findByFirewallId(Long firewallId, Pageable pageable);

    long countByFirewallId(Long firewallId);

    List<Policy> findByFirewallIdOrderByPolicyOrderAsc(Long firewallId);

    Page<Policy> findByFirewallIdOrderByPolicyOrderAsc(Long firewallId, Pageable pageable);

    @Query("SELECT p.policyOrder FROM Policy p WHERE p.firewall.id = :firewallId ORDER BY p.policyOrder ASC")
    List<Integer> findPolicyOrdersByFirewallId(@Param("firewallId") Long firewallId);
}
