package com.smartestate.repo;

import com.smartestate.model.BylawDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BylawDocRepository extends JpaRepository<BylawDoc, Long> {
    List<BylawDoc> findAllByOrderByCodeAsc();
    Optional<BylawDoc> findByCode(String code);
}
