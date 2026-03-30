package ru.korevg.fimas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.korevg.fimas.entity.Zone;
import ru.korevg.fimas.exception.EntityNotFoundException;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {

    Optional<Zone> findByName(String name);
    Optional<Zone> findByNameIgnoreCase(String name);
    boolean existsByName(String name);
    List<Zone> findAllByOrderByPriorityAsc();

    default Optional<Zone> findAnyZone() {
        return findByName("ANY");
    }

    /**
     * Получить зону по имени или выбросить исключение
     */
    default Zone getByName(String name) {
        return findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Zone with name '" + name + "' not found"));
    }

    /**
     * Получить зону ANY или выбросить исключение
     */
    default Zone getAnyZone() {
        return getByName("ANY");
    }
}
