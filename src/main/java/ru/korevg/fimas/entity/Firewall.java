package ru.korevg.fimas.entity;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "firewall")
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

    @Column(name = "mgmt_ip_address", length = 15, nullable = false)
    @NotBlank(message = "IP-адрес обязателен")
    @Pattern(
            regexp = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.){3}(25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)$",
            message = "Некорректный формат IPv4-адреса (ожидается: 0-255.0-255.0-255.0-255 без ведущих нулей в октетах)"
    )
    private String mgmtIpAddress;

    @OneToMany(mappedBy = "firewall", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Policy> policies = new HashSet<>();

    @OneToMany(mappedBy = "firewall", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<DynamicAddress> dynamicAddresses = new HashSet<>();
}
