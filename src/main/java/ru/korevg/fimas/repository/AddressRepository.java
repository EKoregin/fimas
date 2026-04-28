package ru.korevg.fimas.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.korevg.fimas.dto.address.AddressShortResponse;
import ru.korevg.fimas.entity.Address;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    boolean existsByName(String name);

    @Query("""
    SELECT a FROM Address a
    LEFT JOIN a.firewall f
    WHERE LOWER(a.name) LIKE LOWER(:pattern)
       OR LOWER(a.description) LIKE LOWER(:pattern)
       OR LOWER(f.name) LIKE LOWER(:pattern)
       OR EXISTS (
           SELECT 1 FROM a.addresses addr
           WHERE LOWER(addr) LIKE LOWER(:pattern)
       )
    """)
    Page<Address> findBySearchPattern(@Param("pattern") String pattern, Pageable pageable);

    @Query("""
    SELECT COUNT(a) FROM Address a
    LEFT JOIN a.firewall f
    WHERE LOWER(a.name) LIKE LOWER(:pattern)
       OR LOWER(a.description) LIKE LOWER(:pattern)
       OR LOWER(f.name) LIKE LOWER(:pattern)
       OR EXISTS (
           SELECT 1 FROM a.addresses addr
           WHERE LOWER(addr) LIKE LOWER(:pattern)
       )
    """)
    long countBySearchPattern(@Param("pattern") String pattern);

    @Query("SELECT new ru.korevg.fimas.dto.address.AddressShortResponse(a.id, a.name) FROM Address a")
    List<AddressShortResponse> findShortsByFirewallId();

    @Query(value = """
    SELECT a.id, a.name FROM address a
    WHERE a.address_type = 'COMMON'
    
    UNION
    
    SELECT a.id, a.name FROM address a
    JOIN dynamic_address da ON a.id = da.id
    WHERE a.address_type = 'DYNAMIC'
      AND da.firewall_id = :firewallId
    """, nativeQuery = true)
    List<AddressShortResponse> findShortsByFirewallId(@Param("firewallId") Long firewallId);

    @EntityGraph(attributePaths = "addresses")
    @Query("""
            SELECT a FROM Address a
            LEFT JOIN FETCH TREAT(a AS DynamicAddress).firewall fw
            WHERE TYPE(a) = CommonAddress
               OR (TYPE(a) = DynamicAddress AND fw.id = :firewallId)
            """)
    List<Address> findAllByFirewallId(@Param("firewallId") Long firewallId);

    @EntityGraph(attributePaths = "addresses")
    @Query("SELECT a FROM Address a LEFT JOIN FETCH TREAT(a AS DynamicAddress).firewall")
    List<Address> findAllWithFirewall();

}
