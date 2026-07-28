package com.example.KijiList;

import org.springframework.stereotype.Controller; // ※ @RestController から変更
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller  // 文字ではなく「HTML」を返すアノテーションに変更
public class HelloController {

    @GetMapping("/")
    public String home(Model model) {
        // HTML側（th:text="${message}"）に渡す文字を設定します
        model.addAttribute("message", "Java（コントローラー）から届いたメッセージです！");

        // templatesフォルダの中にある「index.html」を呼び出します（拡張子は不要）
        return "index";
    }
}