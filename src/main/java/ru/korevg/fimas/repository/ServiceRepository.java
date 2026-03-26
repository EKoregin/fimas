package ru.korevg.fimas.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.korevg.fimas.dto.service.ServiceShortResponse;
import ru.korevg.fimas.entity.Service;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    Optional<Service> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT new ru.korevg.fimas.dto.service.ServiceShortResponse(service.id, service.name) FROM Service service")
    List<ServiceShortResponse> findAllShort();

    @EntityGraph(attributePaths = {"ports"})
    @Query("SELECT s FROM Service s")
    List<Service> findAllWithPorts();
}