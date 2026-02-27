package ru.korevg.fimas.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "common_address")
@DiscriminatorValue("COMMON")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class CommonAddress extends Address {
}
