package com.example.KijiList.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.example.KijiList.entity.SiteUser;
import com.example.KijiList.repository.SiteUserRepository;

@Service // 💡 これを付けることでSpring Securityが自動的にこのログイン処理を採用します
public class SimpleUserDetailsService implements UserDetailsService {

    private final SiteUserRepository userRepository;

    // コンストラクタ注入（リポジトリを利用できるようにする）
    public SimpleUserDetailsService(SiteUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 💡 入力されたユーザー名でデータベースを検索
        SiteUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + username));

        // 💡 見つかったユーザー情報を、Step 1で作った「Spring Security用の箱」に詰めて返す
        return new SimpleUserDetails(user);
    }
}
