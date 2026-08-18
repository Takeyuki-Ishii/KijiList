package com.example.KijiList;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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
            @RequestParam(value = "filter", defaultValue = "all") String filter,
            @RequestParam(value = "sort", defaultValue = "deadline") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            Model model) {

        // ServiceからPageオブジェクトを取得
        Page<Task> taskPage = taskService.getTasks(page, filter, keyword, sort);

        // Thymeleafに渡すデータをモデルに格納
        model.addAttribute("taskPage", taskPage);                // Pageオブジェクトごと渡す
        model.addAttribute("taskList", taskPage.getContent());    // ループ用（現在のページのタスクリスト）

        // 現在の条件をURLパラメータ維持のためにモデルに保持
        model.addAttribute("currentPage", page);
        model.addAttribute("currentStatus", filter);
        model.addAttribute("currentSortType", sort);
        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("task", new Task());
        return "task-list"; // 一覧画面のHTML名
    }

    // 2.タスクを追加する
    @PostMapping("/add-task")
    public String addTask(
            @Validated @ModelAttribute("task") Task task, // @Validatedや@Validがついている箇所
            BindingResult bindingResult,
            Model model,
            // 現在のページネーションや検索状態も維持するために引数で受け取る
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "deadline") String sort) {

        // 1. バリデーションエラーを検知した場合
        if (bindingResult.hasErrors()) {

            // ★ココが重要！画面を再表示するために、一覧データ（5件分）をもう一度Modelに詰め直す
            Page<Task> taskPage = taskService.getTasks(page, filter, keyword, sort);
            model.addAttribute("taskPage", taskPage);
            model.addAttribute("taskList", taskPage.getContent());

            // URLパラメータ維持のためのデータも詰め直す
            model.addAttribute("currentPage", page);
            model.addAttribute("currentStatus", filter);
            model.addAttribute("currentSortType", sort);
            model.addAttribute("currentKeyword", keyword);

            // 保存（service.save）は呼び出さずに、そのまま入力画面（一覧画面）を返す
            return "task-list";
        }

        // 2. エラーがなければ正常に保存してリダイレクト
        taskService.saveTask(task); // お使いの保存メソッド
        model.addAttribute("currentPage", page);
        model.addAttribute("currentStatus", filter);
        model.addAttribute("currentSortType", sort);
        model.addAttribute("currentKeyword", keyword);
        String encodedKeyword = "";
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 「作業」を「%E4%BD%9C%E6%A5%AD」のような形式に安全に変換します
            encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        }

        // 組み立てるURLには「encodedKeyword」を指定する
        return "redirect:/tasks?page=" + page + "&filter=" + filter + "&sort=" + sort + "&keyword=" + encodedKeyword;
    }

    // 3.タスクを削除する
    @PostMapping("/delete-task/{id}")
    public String deleteTask(@PathVariable Long id,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "filter", required = false) String filter,
                             @RequestParam(value = "sort", required = false) String sort,
                             RedirectAttributes redirectAttributes) {
        taskService.deleteTask(id);
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("keyword", keyword);
        redirectAttributes.addAttribute("filter", filter);
        redirectAttributes.addAttribute("sort", sort);
        return "redirect:/tasks";
    }
    // 4. 編集画面を表示する
    @GetMapping("/tasks/{id}/edit")
    public String showEditForm(
            @PathVariable("id") Long id,
            @RequestParam(value = "page", required = false) int page,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort,
            Model model) {

        // パスに含まれるIDを元に、編集対象のタスクを1件取得する
        // 見つからなかった場合は例外を投げるか、一覧にリダイレクトします
        Task task = taskService.getTaskById(id);
        model.addAttribute("task", task);

        // 【追加】一覧画面から引き継いだ状態を、編集画面のHTMLにも渡す
        model.addAttribute("currentPage", page);
        model.addAttribute("returnKeyword", keyword);
        model.addAttribute("returnFilter", filter);
        model.addAttribute("returnSort", sort);

        return "task-edit";
    }

    // 5. タスクを更新する
    @PostMapping("/update-task/{id}")
    public String updateTask(
            @PathVariable Long id,
            @Validated @ModelAttribute("task") Task task, // @Validated がついているか確認
            BindingResult bindingResult,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "deadline") String sort,
            Model model) {

        // 1. バリデーションエラー（空欄など）を検知した場合
        if (bindingResult.hasErrors()) {
            // 重要度の選択肢（高・中・低）などをモデルに再格納する必要があればここに記述します
            // 例：model.addAttribute("priorities", List.of("高", "中", "低"));

            // URLパラメータ（検索・ソート・フィルター状態）を画面の「キャンセル」ボタン等に引き継ぐためにModelに保持
            model.addAttribute("currentPage", page);
            model.addAttribute("currentStatus", filter);
            model.addAttribute("currentSortType", sort);
            model.addAttribute("currentKeyword", keyword);

            // データベースへは保存（update）せず、そのまま編集画面（task-edit）を呼び出して再表示する
            return "task-edit";
        }

        // 2. エラーがなければ正常に更新処理を行ってリダイレクト
        // ここでServiceの更新メソッド（例：taskService.update(id, task); など）を呼び出す
        taskService.saveTask(task);
        String encodedKeyword = "";
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 「作業」を「%E4%BD%9C%E6%A5%AD」のような形式に安全に変換します
            encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        }
        // リダイレクト先にも現在のページや検索条件を引き継ぐ
        return "redirect:/tasks?page=" + page + "&filter=" + filter + "&sort=" + sort + "&keyword=" + encodedKeyword;
    }

    // 6.完了フラグをONにする
    @PostMapping("/complete-task/{id}")
    public String completeTask(@PathVariable Long id,
                               @RequestParam(value = "page", required = false) int page,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "filter", required = false) String filter,
                               @RequestParam(value = "sort", required = false) String sort,
                               RedirectAttributes redirectAttributes) { // ★追加

        // 完了フラグ更新処理
        taskService.completeTask(id);

        // ★リダイレクト先に現在の状態を引き継ぐ
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("keyword", keyword);
        redirectAttributes.addAttribute("filter", filter);
        redirectAttributes.addAttribute("sort", sort);

        return "redirect:/tasks";// ※既存の一覧画面のURLに合わせて変更してください
    }
    // 既存のクラス内に配置してください
    @PostMapping("/tasks/bulk-delete")
    public String bulkDeleteTasks(
            @RequestParam(value = "taskIds", required = false) List<Long> taskIds,
            @RequestParam(value = "page", required = false) int page,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "filter", defaultValue = "ALL") String filter,
            @RequestParam(value = "sort", defaultValue = "deadline") String sort,
            // もしページネーションのページ番号も維持したい場合は以下を生かしてください
            // @RequestParam(value = "page", defaultValue = "0") int page,
            RedirectAttributes redirectAttributes) {

        // 1. チェックボックスが1つ以上選択されている場合のみ削除処理を実行
        if (taskIds != null && !taskIds.isEmpty()) {
            taskService.deleteTasks(taskIds); // ※このあとService側に実装します
            redirectAttributes.addFlashAttribute("successMessage", taskIds.size() + "件のタスクを一括削除しました。");
        }

        // 2. 現在の「検索窓・フィルター・ソート」の状態を維持して一覧画面にリダイレクト
        String encodedKeyword = "";
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 「作業」を「%E4%BD%9C%E6%A5%AD」のような形式に安全に変換します
            encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        }

        // 組み立てるURLには「encodedKeyword」を指定する
        return "redirect:/tasks?page=" + page + "&filter=" + filter + "&sort=" + sort + "&keyword=" + encodedKeyword;
    }
}