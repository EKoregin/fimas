package ru.korevg.fimas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.korevg.fimas.entity.Address;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    boolean existsByName(String name);
}
