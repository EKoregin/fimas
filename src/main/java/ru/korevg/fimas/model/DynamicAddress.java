package ru.korevg.fimas.model;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dynamic_addresses")
@DiscriminatorValue("DYNAMIC")
@Getter
@Setter
public class DynamicAddress extends Address {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "firewall_id", nullable = false)
    private Firewall firewall;

    public DynamicAddress() {}
    public DynamicAddress(Firewall firewall) {
        this.firewall = firewall;
    }
}
