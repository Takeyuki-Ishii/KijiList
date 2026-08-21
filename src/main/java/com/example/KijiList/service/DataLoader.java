package com.example.KijiList.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.example.KijiList.entity.SiteUser;
import com.example.KijiList.repository.SiteUserRepository;

@Component // 💡 Spring Boot起動時に自動的に読み込まれます
public class DataLoader implements CommandLineRunner {

    private final SiteUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // コンストラクタ注入（リポジトリとパスワードエンコーダーを呼び出す）
    public DataLoader(SiteUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 💡 既にテストユーザーが存在しない場合のみ登録する（起動するたびに重複登録されるのを防ぐ）
        if (userRepository.findByUsername("user").isEmpty()) {
            SiteUser testUser = new SiteUser();
            testUser.setUsername("user");

            // 💡 ここで重要！「password123」をBCryptでハッシュ化してセットします
            testUser.setPassword(passwordEncoder.encode("password123"));

            // 権限を設定（Spring Securityで一般的な一般ユーザー権限）
            testUser.setRole("ROLE_USER");

            // データベースに保存
            userRepository.save(testUser);
            System.out.println("--- 🔑 テストユーザー(user/password123)をデータベースに登録しました ---");
        }
    }
}
