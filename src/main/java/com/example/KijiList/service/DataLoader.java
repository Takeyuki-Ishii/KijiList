package com.example.KijiList.service;

import com.example.KijiList.Task;
import com.example.KijiList.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.example.KijiList.entity.SiteUser;
import com.example.KijiList.repository.SiteUserRepository;

import java.time.LocalDate;

@Component // 💡 Spring Boot起動時に自動的に読み込まれます
public class DataLoader implements CommandLineRunner {
    @Autowired
    private SiteUserRepository siteUserRepository;
    @Autowired
    private TaskRepository taskRepository;
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
        if (taskRepository.count() == 0) {
            SiteUser testUser = new SiteUser();
            testUser.setUsername("user_test");
            testUser.setPassword(passwordEncoder.encode("password123"));
            testUser.setRole("ROLE_USER");
            SiteUser savedUser = userRepository.save(testUser);
            // テストタスク1
            Task task1 = new Task();
            task1.setName("お買い物（牛乳と卵）");
            task1.setDeadline(LocalDate.now().plusDays(2)); // 2日後
            task1.setPriority("中");
            task1.setMemo("夕方までにスーパーに行くこと");
            task1.setCompleted(false);
            task1.setSiteUser(savedUser); // 💡最重要：上で登録・取得したsavedUserを紐付ける！

            taskRepository.save(task1);

            // テストタスク2（期限が今日、重要度「高」のサンプル）
            Task task2 = new Task();
            task2.setName("Spring Bootの課題提出");
            task2.setDeadline(LocalDate.now()); // 今日
            task2.setPriority("高");
            task2.setMemo("17時までにGitにプッシュする");
            task2.setCompleted(false);
            task2.setSiteUser(savedUser); // こちらも同じくsavedUserを紐付ける！

            taskRepository.save(task2);
        }
    }
}
