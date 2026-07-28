package com.example.KijiList;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // ★これが消えていたため、エラーになっていました！
public class Test06Application {

	public static void main(String[] args) {
		SpringApplication.run(Test06Application.class, args);
	}
}