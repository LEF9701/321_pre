package com.smartestate.repo;

import com.smartestate.model.Issue;
import com.smartestate.model.IssueStatus;
import com.smartestate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findAllByOrderByCreatedAtDesc();
    List<Issue> findByReporterOrderByCreatedAtDesc(User reporter);
    List<Issue> findByStatusOrderByCreatedAtDesc(IssueStatus status);
    long countByStatusNot(IssueStatus status);
    long countByUnit_Floor_Block_IdAndStatusNot(Long blockId, IssueStatus status);
    List<Issue> findByUnit_IdAndStatusNot(Long unitId, IssueStatus status);
}
