package ru.korevg.fimas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.korevg.fimas.entity.Firewall;

import java.util.List;
import java.util.Optional;

@Repository
public interface FirewallRepository extends JpaRepository<Firewall, Long> {

    Optional<Firewall> findByName(String name);
    boolean existsByName(String name);
    List<Firewall> findByModelVendorName(String vendorName);
}
