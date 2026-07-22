package com.smartestate.repo;

import com.smartestate.model.DeviceToken;
import com.smartestate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    /** Active push destinations for a resident: the tokens the dispatcher pushes to. */
    List<DeviceToken> findByUserAndActiveTrue(User user);

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUser(User user);
}
