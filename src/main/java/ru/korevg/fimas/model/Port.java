package ru.korevg.fimas.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "ports")
public class Port {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private Protocol protocol;

    private Integer port;
}
