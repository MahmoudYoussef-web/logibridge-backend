package com.logibridge.backend.auth.entity;

import com.logibridge.backend.auth.enums.RoleName;
import com.logibridge.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Entity
@Where(clause = "is_deleted = false")
@Table(
        name = "roles",
        indexes = {
                @Index(name = "idx_role_name", columnList = "name", unique = true)
        }
)
public class Role extends BaseEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 40)
    private RoleName name;

    @Column(name = "description", length = 255)
    private String description;
}