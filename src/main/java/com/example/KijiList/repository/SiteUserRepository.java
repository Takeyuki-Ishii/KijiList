package com.example.KijiList.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.KijiList.entity.SiteUser;

public interface SiteUserRepository extends JpaRepository<SiteUser, Long> {

    // 💡 ログイン処理で「ユーザー名からアカウントを探す」ためのカスタムメソッド
    Optional<SiteUser> findByUsername(String username);
}
