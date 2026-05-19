package com.att.tdp.issueflow.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenBlocklistRepository extends JpaRepository<TokenBlocklist, Long> {
    boolean existsByJti(String jti);
}
