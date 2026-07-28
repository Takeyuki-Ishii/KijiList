package com.example.KijiList;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid; // ★追加
import java.util.List;
import org.springframework.validation.BindingResult; // ★追加
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute; // ★追加
import org.springframework.web.bind.annotation.PathVariable; // ★追加


@Controller
public class TaskController {

    // 修正：Repository ではなく Service を呼び出すように変更
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // 1.タスク一覧を表示する
    @GetMapping("/tasks")
    public String listTasks(
            @RequestParam(name = "sort", required = false, defaultValue = "deadline") String sort,
            @RequestParam(name = "filter", required = false, defaultValue = "all") String filter, // 💡 追加
            Model model) {

        // 💡 Serviceの新しいメソッドに2つの条件を渡す
        List<Task> tasks = taskService.getFilteredAndSortedTasks(filter, sort);

        model.addAttribute("tasks", tasks);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentFilter", filter); // 💡 現在のフィルター状態を画面に送る
        model.addAttribute("task", new Task()); // 前回のWhitelabelエラー対策の1行

        return "task-list";
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

    // 3.タスクを削除する（変更なし）
    @PostMapping("/delete-task/{id}")
    public String deleteTask(@PathVariable Long id) { // 必ず `@PathVariable` をつける
        taskService.deleteTask(id);
        return "redirect:/tasks";
    }
    // 4. 編集画面を表示する
    @GetMapping("/edit-task/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        // パスに含まれるIDを元に、編集対象のタスクを1件取得する
        // 見つからなかった場合は例外を投げるか、一覧にリダイレクトします
        Task task = taskService.getTaskById(id);

        model.addAttribute("task", task); // 取得したタスクを画面に渡す
        return "task-edit"; // 編集用の新しいHTMLを表示
    }

    // 5. タスクを更新する
    @PostMapping("/update-task/{id}")
    public String updateTask(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("task") Task task,
                             BindingResult bindingResult) {

        // バリデーションエラーがあった場合は、編集画面に戻す
        if (bindingResult.hasErrors()) {
            return "task-edit";
        }

        // 送られてきたオブジェクトに既存のIDをセットして保存（save）すると、
        // 新規追加ではなく「上書き更新」になります
        task.setId(id);
        taskService.saveTask(task);

        return "redirect:/tasks"; // 更新が終わったら一覧に戻る
    }

    // 6.完了フラグをONにする
    @PostMapping("/complete-task/{id}")
    public String completeTask(@PathVariable Long id) {

        taskService.completeTask(id);
        // 一覧画面にリダイレクト
        return "redirect:/tasks"; // ※既存の一覧画面のURLに合わせて変更してください
    }
}