package ru.korevg.fimas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "action")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToMany
    @JoinTable(
            name = "action_command",
            joinColumns = @JoinColumn(name = "action_id"),
            inverseJoinColumns = @JoinColumn(name = "command_id")
    )
//    @OrderColumn(name = "command_order")
    @Builder.Default
    private Set<Command> commands = new LinkedHashSet<>();

//    @ManyToMany
//    @JoinTable(
//            name = "action_command",
//            joinColumns = @JoinColumn(name = "action_id"),
//            inverseJoinColumns = @JoinColumn(name = "command_id")
//    )
//    @OrderColumn(name = "command_order")
//    @Builder.Default
//    private List<Command> commands = new ArrayList<>();
}
