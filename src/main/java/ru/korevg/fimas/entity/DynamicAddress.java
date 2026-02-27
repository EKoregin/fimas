package ru.korevg.fimas.entity;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dynamic_address")
@DiscriminatorValue("DYNAMIC")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynamicAddress extends Address {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "firewall_id", nullable = false)
    private Firewall firewall;
}
