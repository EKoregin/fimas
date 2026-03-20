package ru.korevg.fimas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.korevg.fimas.entity.Model;

import java.util.Optional;

@Repository
public interface ModelRepository extends JpaRepository<Model, Long> {

    Optional<Model> findByNameAndVendorId(String name, Long vendorId);

    boolean existsByNameAndVendorId(String name, Long vendorId);

    @Query("""
                SELECT DISTINCT m 
                FROM Model m 
                LEFT JOIN FETCH m.actions a 
                LEFT JOIN FETCH a.commands 
                WHERE m.id = :modelId
            """)
    Optional<Model> findByIdWithActions(Long modelId);
}