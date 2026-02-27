package ru.korevg.fimas.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "firewalls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Firewall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Policy> policies = new HashSet<>();

    @OneToMany(mappedBy = "firewall", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<DynamicAddress> dynamicAddresses = new HashSet<>();
}
