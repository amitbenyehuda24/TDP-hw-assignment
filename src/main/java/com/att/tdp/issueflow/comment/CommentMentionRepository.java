package com.att.tdp.issueflow.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentMentionRepository extends JpaRepository<CommentMention, Long> {

    List<CommentMention> findAllByCommentId(Long commentId);

    void deleteAllByCommentId(Long commentId);
}
