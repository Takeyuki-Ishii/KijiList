package com.example.KijiList.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "site_user") // Render（PostgreSQL）を見据え、テーブル名は小修飾・スネークケースが安全です
public class SiteUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ログイン時に使用する一意のユーザー名（メールアドレスでも可）
    @Column(unique = true, nullable = false)
    private String username;

    // BCryptでハッシュ化されたパスワードを保存するカラム
    @Column(nullable = false)
    private String password;

    // ユーザーの権限（一般ユーザー：ROLE_USER、管理者：ROLE_ADMIN など）
    @Column(nullable = false)
    private String role;

    // --- ゲッター・セッター（またはLombokの@Data）を以下に記述してください ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
