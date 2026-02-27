package ru.korevg.fimas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.korevg.fimas.entity.Port;

import java.util.Optional;

@Repository
public interface PortRepository extends JpaRepository<Port, Long> {

    Optional<Port> findByProtocolAndDstPort(String protocol, String dstPort);

    boolean existsByProtocolAndDstPort(String protocol, String dstPort);
}