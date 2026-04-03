package com.logibridge.backend.auth.entity;

import com.logibridge.backend.auth.enums.UserStatus;
import com.logibridge.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(exclude = "userRoles")
@Entity
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE id=?")
@Where(clause = "is_deleted = false")
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_email", columnList = "email", unique = true),
                @Index(name = "idx_user_status", columnList = "status"),
                @Index(name = "idx_user_created", columnList = "created_at")
        }
)
public class User extends BaseEntity {

    @NotBlank
    @Size(min = 2, max = 50)
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 50)
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Size(max = 20)
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Builder.Default
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked = true;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRole> userRoles = new ArrayList<>();


    public void addRole(Role role) {
        if (role == null) return;

        boolean exists = userRoles.stream()
                .anyMatch(ur -> ur.getRole().getName() == role.getName());

        if (exists) return;

        UserRole userRole = UserRole.builder()
                .user(this)
                .role(role)
                .build();

        userRoles.add(userRole); // owning side is UserRole → enough
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @PrePersist
    @PreUpdate
    public void normalizeEmail() {
        if (email != null) {
            email = email.toLowerCase().trim();
        }
    }
}