package ru.korevg.fimas.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "common_addresses")
@DiscriminatorValue("COMMON")
public class CommonAddress extends Address {
}
