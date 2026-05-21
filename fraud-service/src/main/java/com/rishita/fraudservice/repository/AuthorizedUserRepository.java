package com.rishita.fraudservice.repository;

import com.rishita.fraudservice.entity.AuthorizedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AuthorizedUserRepository extends JpaRepository<AuthorizedUser, Long> {
    @Query("select a from AuthorizedUser a where a.userId = ?1")
    Optional<AuthorizedUser> findByUserId(Long userId);
}
