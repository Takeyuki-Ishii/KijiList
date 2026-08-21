package com.example.KijiList.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ① アクセス権限の設定
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll() // 静的ファイル
                        .requestMatchers("/signup", "/login").permitAll()               // ログイン・会員登録画面
                        .requestMatchers("/h2-console/**").permitAll()                 // H2 DB（開発用）
                        .anyRequest().authenticated()                                   // その他は要ログイン
                )
                // ② 自作ログイン画面の設定
                .formLogin(form -> form
                        .loginPage("/login")               // 自作ログイン画面のURL
                        .loginProcessingUrl("/login")       // ログインフォームのth:actionと一致させる
                        .defaultSuccessUrl("/tasks", true)  // 成功時の遷移先
                        .failureUrl("/login?error")        // 失敗時の遷移先（エラー表示用）
                        .permitAll()                       // ログイン処理自体も全員に許可
                )
                // ③ ログアウトの設定
                .logout(logout -> logout
                        .logoutUrl("/logout")               // ログアウトを実行するURL
                        .logoutSuccessUrl("/login?logout")  // 成功後の遷移先
                        .invalidateHttpSession(true)        // セッションの破棄
                        .deleteCookies("JSESSIONID")        // クッキーの削除
                        .permitAll()
                );

        // H2コンソール用（Render本番環境では不要になりますが、一旦開発用に残します）
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}