package ru.korevg.fimas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.korevg.fimas.dto.port.PortShortProjection;
import ru.korevg.fimas.dto.port.PortShortResponse;
import ru.korevg.fimas.entity.Port;
import ru.korevg.fimas.entity.Protocol;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortRepository extends JpaRepository<Port, Long> {

    Optional<Port> findByProtocolAndDstPort(Protocol protocol, String dstPort);

    boolean existsByProtocolAndDstPort(Protocol protocol, String dstPort);

    @Query("SELECT p.id AS id, p.protocol AS protocol, p.srcPort AS srcPort, p.dstPort AS dstPort " +
            "FROM Port p ORDER BY p.dstPort")
    List<PortShortProjection> findAllShort();
}