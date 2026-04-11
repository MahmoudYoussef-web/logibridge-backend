package com.logibridge.backend.auth.repository;

import com.logibridge.backend.auth.entity.User;
import com.logibridge.backend.auth.enums.RoleName;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.userRoles ur
        LEFT JOIN FETCH ur.role
        WHERE u.email = :email
    """)
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    boolean existsByEmail(String email);



    @Query("""
    SELECT DISTINCT u FROM User u
    JOIN u.userRoles ur
    JOIN ur.role r
    WHERE r.name = :roleName
      AND u.enabled = true
      AND u.accountNonLocked = true
""")
    List<User> findAllActiveUsersByRole(@Param("roleName") RoleName roleName);
}