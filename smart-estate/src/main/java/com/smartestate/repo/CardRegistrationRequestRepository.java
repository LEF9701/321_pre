package com.smartestate.repo;

import com.smartestate.model.CardRegistrationRequest;
import com.smartestate.model.CardRequestStatus;
import com.smartestate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRegistrationRequestRepository extends JpaRepository<CardRegistrationRequest, Long> {
    List<CardRegistrationRequest> findByStatusOrderByCreatedAtAsc(CardRequestStatus status);
    List<CardRegistrationRequest> findByRequesterOrderByCreatedAtDesc(User requester);
}
