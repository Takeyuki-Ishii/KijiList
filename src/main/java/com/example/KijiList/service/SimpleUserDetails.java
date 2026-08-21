package com.example.KijiList.service;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.example.KijiList.entity.SiteUser;

public class SimpleUserDetails implements UserDetails {

    private final SiteUser siteUser;

    // コンストラクタで自作のSiteUserを受け取る
    public SimpleUserDetails(SiteUser siteUser) {
        this.siteUser = siteUser;
    }

    // 💡 データベースに保存されている権限（ROLE_USERなど）をSpring Security用に変換して返す
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(siteUser.getRole()));
    }

    // 💡 データベースのパスワードを返す
    @Override
    public String getPassword() {
        return siteUser.getPassword();
    }

    // 💡 データベースのユーザー名を返す
    @Override
    public String getUsername() {
        return siteUser.getUsername();
    }

    // --- 以下はアカウントの有効期限などの設定です。今回はすべて「常に有効（true）」にします ---
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
