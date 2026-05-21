package com.rishita.fraudservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "authorized_user")
public class AuthorizedUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", unique = true, nullable = false)
    private Long userId;

    public AuthorizedUser() {}
    public AuthorizedUser(Long userId) { this.userId = userId; }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
