package ru.korevg.fimas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import ru.korevg.fimas.entity.Firewall;
import ru.korevg.fimas.repository.FirewallRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class FirewallRepositoryTest {

    @Autowired
    private FirewallRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldFindByName() {
        // given
        Firewall firewall = new Firewall();
        firewall.setName("test-fw");
        entityManager.persistAndFlush(firewall);

        // when
        Optional<Firewall> found = repository.findByName("test-fw");

        // then
        assertTrue(found.isPresent());
        assertEquals("test-fw", found.get().getName());
    }

    @Test
    void shouldReturnTrueWhenNameExists() {
        Firewall firewall = new Firewall();
        firewall.setName("existing");
        entityManager.persistAndFlush(firewall);

        assertTrue(repository.existsByName("existing"));
    }
}