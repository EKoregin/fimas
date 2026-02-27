package ru.korevg.fimas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.korevg.fimas.entity.Model;

import java.util.Optional;

@Repository
public interface ModelRepository extends JpaRepository<Model, Long> {

    Optional<Model> findByNameAndVendorId(String name, Long vendorId);

    boolean existsByNameAndVendorId(String name, Long vendorId);
}