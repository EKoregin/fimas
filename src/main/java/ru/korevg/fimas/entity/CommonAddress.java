package ru.korevg.fimas.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "common_address")
@DiscriminatorValue("COMMON")
@Getter
@Setter
@NoArgsConstructor
public class CommonAddress extends Address {
}
