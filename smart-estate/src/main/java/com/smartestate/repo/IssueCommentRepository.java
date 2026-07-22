package com.smartestate.repo;

import com.smartestate.model.Issue;
import com.smartestate.model.IssueComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IssueCommentRepository extends JpaRepository<IssueComment, Long> {
    List<IssueComment> findByIssueOrderByCreatedAtAsc(Issue issue);
}
