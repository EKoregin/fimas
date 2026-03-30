package ru.korevg.fimas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "zone")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer priority;

    @ElementCollection
    @CollectionTable(name = "zone_interfaces",
            joinColumns = @JoinColumn(name = "zone_id"))
    @Column(name = "interface_name", length = 100)
    @Builder.Default
    private List<String> interfaces = new ArrayList<>();
}