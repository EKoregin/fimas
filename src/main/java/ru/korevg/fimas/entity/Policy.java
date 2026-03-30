package ru.korevg.fimas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "src_zone_id")
    private Zone srcZone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dst_zone_id")
    private Zone dstZone;

    @ManyToMany
    @JoinTable(
            name = "policy_src_addresses",
            joinColumns = @JoinColumn(name = "policy_id"),
            inverseJoinColumns = @JoinColumn(name = "address_id")
    )
    @Builder.Default
    private Set<Address> srcAddresses = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "policy_dst_addresses",
            joinColumns = @JoinColumn(name = "policy_id"),
            inverseJoinColumns = @JoinColumn(name = "address_id")
    )
    @Builder.Default
    private Set<Address> dstAddresses =  new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "policy_services",
            joinColumns = @JoinColumn(name = "policy_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @Builder.Default
    private Set<Service> services =  new HashSet<>();

    @Enumerated(EnumType.STRING)
    private PolicyAction action;

    @Enumerated(EnumType.STRING)
    private PolicyStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "firewall_id", nullable = false)
    private Firewall firewall;

    @Min(0)
    @Column(name = "policy_order", nullable = false)
    private Integer policyOrder;

//    public void addSrcAddress(Address a) {
//        srcAddresses.add(a);
//    }
//
//    public void removeSrcAddress(Address a) {
//        srcAddresses.remove(a);
//    }
//
//    public void addDstAddress(Address a) {
//        dstAddresses.add(a);
//    }
//
//    public void removeDstAddress(Address a) {
//        dstAddresses.remove(a);
//    }
}
