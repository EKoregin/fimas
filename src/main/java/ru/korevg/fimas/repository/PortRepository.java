package ru.korevg.fimas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.korevg.fimas.entity.Port;
import ru.korevg.fimas.entity.Protocol;

import java.util.Optional;

@Repository
public interface PortRepository extends JpaRepository<Port, Long> {

    Optional<Port> findByProtocolAndDstPort(Protocol protocol, String dstPort);

    boolean existsByProtocolAndDstPort(Protocol protocol, String dstPort);
}