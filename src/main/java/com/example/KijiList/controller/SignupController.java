package com.example.KijiList.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.KijiList.entity.SiteUser;
import com.example.KijiList.repository.SiteUserRepository;

@Controller
public class SignupController {

    private final SiteUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // コンストラクタ注入
    public SignupController(SiteUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ① 会員登録画面を表示する
    @GetMapping("/signup")
    public String signupForm() {
        return "signup"; // signup.html を呼び出す
    }

    // ② 会員登録処理を実行する
    @PostMapping("/signup")
    public String registerUser(@RequestParam("username") String username,
                               @RequestParam("password") String password) {

        // 簡易的な重複チェック（すでに同じユーザー名が登録されていたら登録画面に戻す）
        if (userRepository.findByUsername(username).isPresent()) {
            return "redirect:/signup?error";
        }

        // 新しいユーザーエンティティを作成
        SiteUser newUser = new SiteUser();
        newUser.setUsername(username);

        // 💡 入力されたパスワードをしっかりハッシュ化（暗号化）して保存！
        newUser.setPassword(passwordEncoder.encode(password));

        // 一般ユーザー権限を付与
        newUser.setRole("ROLE_USER");

        // データベースに保存
        userRepository.save(newUser);

        // 登録が成功したらログイン画面にリダイレクト
        return "redirect:/login?success";
    }
}
