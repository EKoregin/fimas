package ru.korevg.fimas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.korevg.fimas.entity.Command;
import ru.korevg.fimas.entity.CommandType;

import java.util.List;

@Repository
public interface CommandRepository extends JpaRepository<Command, Long> {

    // Найти все команды конкретного вендора
    List<Command> findByVendorId(Long vendorId);

    // Найти команды по типу (SSH / HTTPS)
    List<Command> findByCommandType(CommandType commandType);

    boolean existsById(Long id);

}
