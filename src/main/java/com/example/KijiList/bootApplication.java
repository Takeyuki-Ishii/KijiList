package com.example.KijiList;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing // 追加
@SpringBootApplication //
public class bootApplication {

	public static void main(String[] args) {
		SpringApplication.run(bootApplication.class, args);
	}
}