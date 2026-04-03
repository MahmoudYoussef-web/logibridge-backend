package com.logibridge.backend.auth.repository;

import com.logibridge.backend.auth.entity.Role;
import com.logibridge.backend.auth.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}