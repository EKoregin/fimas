package ru.korevg.fimas.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "address_type")
@Table(name = "address")
@Getter
@Setter
@Slf4j
public abstract class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "sub_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private AddressSubType subType;

    @ElementCollection
    @CollectionTable(
            name = "inet_addresses",
            joinColumns = @JoinColumn(name = "address_id")
    )
    @Column(name = "inet_address", nullable = false, length = 512)
    private Set<String> addresses = new HashSet<>();

    @Column(name = "address_type", insertable = false, updatable = false)
    private String addressType;

    // опционально — геттер с вычисляемым значением
    public String getAddressType() {
        return this instanceof CommonAddress ? "COMMON" : "DYNAMIC";
    }
}
