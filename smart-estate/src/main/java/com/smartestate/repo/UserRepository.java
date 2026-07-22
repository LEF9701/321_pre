package com.smartestate.repo;

import com.smartestate.model.Role;
import com.smartestate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByRoleOrderByNameAsc(Role role);
    long countByRole(Role role);
}
