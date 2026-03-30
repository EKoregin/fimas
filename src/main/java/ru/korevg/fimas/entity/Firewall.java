package ru.korevg.fimas.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    @OneToMany(mappedBy = "firewall",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderColumn(name = "policy_order")
    @Builder.Default
    private List<Policy> policies = new ArrayList<>();

    @OneToMany(mappedBy = "firewall", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<DynamicAddress> dynamicAddresses = new HashSet<>();


    public void addPolicy(Policy policy) {
        if (policy == null) return;
        policies.add(policy);
        policy.setFirewall(this);
        reorderPolicies();
    }

    public void removePolicy(Policy policy) {
        if (policy == null) return;
        policies.remove(policy);
        policy.setFirewall(null);
        reorderPolicies();
    }

    public void addPolicyAtPosition(Policy policy, Integer position) {
        if (policy == null) return;

        // Если политика уже в списке — сначала удаляем (перемещение)
        policies.remove(policy);

        if (position == null || position >= policies.size()) {
            policies.add(policy);                    // в конец
        } else {
            policies.add(position, policy);          // вставка с автоматическим сдвигом
        }

        policy.setFirewall(this);
        reorderPolicies();   // ← САМОЕ ВАЖНОЕ ИЗМЕНЕНИЕ
    }

    /**
     * Пересчитывает policyOrder у всех политик в соответствии с их позицией в списке.
     * Автоматически удаляет null-элементы (защита от коррупции данных).
     */
    private void reorderPolicies() {
        if (policies == null) {
            policies = new ArrayList<>();
            return;
        }

        int index = 0;
        for (int i = 0; i < policies.size(); i++) {
            Policy policy = policies.get(i);
            if (policy != null) {
                policy.setPolicyOrder(index++);
            } else {
                // null попал в список — удаляем его из коллекции
                policies.remove(i);
                i--; // корректируем индекс после удаления
            }
        }
    }
}
