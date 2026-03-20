package ru.korevg.fimas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.korevg.fimas.entity.Action;

import java.util.Optional;

@Repository
public interface ActionRepository extends JpaRepository<Action, Long> {

    // Найти Action по имени (удобно для тестов и инициализации)
    Optional<Action> findByName(String name);

    // Загрузка Action вместе со всеми командами (EAGER fetch)
    @Query("SELECT a FROM Action a LEFT JOIN FETCH a.commands WHERE a.id = :id")
    Optional<Action> findByIdWithCommands(Long id);

    boolean existsByCommandsId(Long commandId);
}