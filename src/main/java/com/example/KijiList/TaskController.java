package com.example.KijiList;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid; // ★追加
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult; // ★追加
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute; // ★追加
import org.springframework.web.bind.annotation.PathVariable; // ★追加
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class TaskController {

    @Autowired
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // 1.タスク一覧を表示する
    @GetMapping("/tasks")
    public String listTasks(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "filter", defaultValue = "all") String filter,
            @RequestParam(value = "sort", defaultValue = "deadline") String sort,
            Model model) {

        // 条件に合うタスク一覧を取得
        List<Task> tasks = taskService.getTasks(keyword, filter, sort);

        // 画面描画用データのセット
        model.addAttribute("tasks", tasks);

        // 【重要】現在の状態をすべてViewに渡す（URLパラメータの維持に必須）
        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("currentFilter", filter);
        model.addAttribute("currentSort", sort);
        // ※昨日までの実装で「task」以外の名前（例: taskForm）にしている場合はその名前に合わせてください
        model.addAttribute("task", new Task());
        return "task-list"; // タスク一覧のHTML名
    }

    // 2.タスクを追加する
    @PostMapping("/add-task")
    public String addTask(@Valid @ModelAttribute("task") Task task, // ★@Validでチェック、オブジェクトで受け取る
                          BindingResult bindingResult,            // ★エラー結果が入る箱（※Taskの直後に配置が必須）
                          Model model) {

        // ★もし入力エラー（バリデーション違反）があった場合
        if (bindingResult.hasErrors()) {
            // エラー表示を残したまま一覧画面を再表示するため、現在のリストを再度詰め直す
            model.addAttribute("tasks", taskService.getAllTasks());
            return "task-list"; // リダイレクトではなく、直接HTMLを表示してエラーを出す
        }

        // エラーがなければデータベースに保存
        taskService.saveTask(task);
        return "redirect:/tasks";
    }

    // 3.タスクを削除する
    @PostMapping("/delete-task/{id}")
    public String deleteTask(@PathVariable Long id,
                             @RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "filter", required = false) String filter,
                             @RequestParam(value = "sort", required = false) String sort,
                             RedirectAttributes redirectAttributes) {
        taskService.deleteTask(id);
        redirectAttributes.addAttribute("keyword", keyword);
        redirectAttributes.addAttribute("filter", filter);
        redirectAttributes.addAttribute("sort", sort);
        return "redirect:/tasks";
    }
    // 4. 編集画面を表示する
    @GetMapping("/tasks/{id}/edit")
    public String showEditForm(
            @PathVariable("id") Long id,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort,
            Model model) {

        // パスに含まれるIDを元に、編集対象のタスクを1件取得する
        // 見つからなかった場合は例外を投げるか、一覧にリダイレクトします
        Task task = taskService.getTaskById(id);
        model.addAttribute("task", task);

        // 【追加】一覧画面から引き継いだ状態を、編集画面のHTMLにも渡す
        model.addAttribute("returnKeyword", keyword);
        model.addAttribute("returnFilter", filter);
        model.addAttribute("returnSort", sort);

        return "task-edit";
    }

    // 5. タスクを更新する
    @PostMapping("/update-task/{id}")
    public String updateTask(
            @PathVariable("id") Long id,
            @ModelAttribute("task") Task task, // バリデーションがある場合は @Validated や BindingResult もここにあります
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort,
            RedirectAttributes redirectAttributes,
            BindingResult bindingResult) {

        // バリデーションエラーがあった場合は、編集画面に戻す
        if (bindingResult.hasErrors()) {
            return "task-edit";
        }
        // 記事の更新処理
        task.setId(id);
        taskService.saveTask(task);

        // ★自動でURLパラメータに変換してリダイレクト先に付与してくれる
        redirectAttributes.addAttribute("keyword", keyword);
        redirectAttributes.addAttribute("filter", filter);
        redirectAttributes.addAttribute("sort", sort);

        return "redirect:/tasks"; // 後ろに自動で ?keyword=... が付きます
    }

    // 6.完了フラグをONにする
    @PostMapping("/complete-task/{id}")
    public String completeTask(@PathVariable Long id,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "filter", required = false) String filter,
                               @RequestParam(value = "sort", required = false) String sort,
                               RedirectAttributes redirectAttributes) { // ★追加

        // 完了フラグ更新処理
        taskService.completeTask(id);

        // ★リダイレクト先に現在の状態を引き継ぐ
        redirectAttributes.addAttribute("keyword", keyword);
        redirectAttributes.addAttribute("filter", filter);
        redirectAttributes.addAttribute("sort", sort);

        return "redirect:/tasks";// ※既存の一覧画面のURLに合わせて変更してください
    }
}