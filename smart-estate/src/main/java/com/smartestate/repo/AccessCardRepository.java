package com.smartestate.repo;

import com.smartestate.model.AccessCard;
import com.smartestate.model.CardStatus;
import com.smartestate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccessCardRepository extends JpaRepository<AccessCard, Long> {
    Optional<AccessCard> findByCardNumber(String cardNumber);
    List<AccessCard> findByOwnerOrderByRegisteredAtDesc(User owner);
    Optional<AccessCard> findFirstByOwnerAndStatusOrderByRegisteredAtDesc(User owner, CardStatus status);
}
