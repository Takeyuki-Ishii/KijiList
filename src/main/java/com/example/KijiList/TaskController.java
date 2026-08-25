package com.example.KijiList;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.example.KijiList.entity.SiteUser;
import com.example.KijiList.service.SimpleUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    // タスク一覧を表示する
    @GetMapping("/tasks")
    public String listTasks(
            @RequestParam(value = "filter", defaultValue = "all") String filter,
            @RequestParam(value = "sort", defaultValue = "deadline") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @AuthenticationPrincipal SimpleUserDetails userDetails, // ★追加：ログインユーザーの取得
            Model model) {

        // ★ログインユーザーのEntityを取得
        SiteUser loginUser = userDetails.getUser();

        // ★Serviceの引数に loginUser を追加
        Page<Task> taskPage = taskService.getTasks(loginUser.getId(), page, filter, keyword, sort);

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

    // タスクを追加する
    @PostMapping("/add-task")
    public String addTask(
            @Validated @ModelAttribute("task") Task task,
            BindingResult bindingResult,
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "all") String filter, // 大文字小文字のズレを防ぐため小文字推奨（コントローラーのデフォルト値に合わせる）
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "deadline") String sort,
            @AuthenticationPrincipal SimpleUserDetails userDetails)// ★追加：ログインユーザーの取得
    {
        // ★ログインユーザーのEntityを取得
        SiteUser loginUser = userDetails.getUser();

        // 1. バリデーションエラーを検知した場合
        if (bindingResult.hasErrors()) {

            // ★修正：一覧データの再取得時にも loginUser を渡す（4連動を維持するため）
            Page<Task> taskPage = taskService.getTasks(loginUser.getId(), page, filter, keyword, sort);
            model.addAttribute("taskPage", taskPage);
            model.addAttribute("taskList", taskPage.getContent());

            // URLパラメータ維持のためのデータも詰め直す
            model.addAttribute("currentPage", page);
            model.addAttribute("currentStatus", filter);
            model.addAttribute("currentSortType", sort);
            model.addAttribute("currentKeyword", keyword);

            return "task-list";
        }

        // 2. エラーがなければ正常に保存してリダイレクト
        // ★重要：画面から送られてきたタスクオブジェクトに、現在のログインユーザーを紐付ける
        task.setSiteUser(loginUser);

        taskService.saveTask(task);

        model.addAttribute("currentPage", page);
        model.addAttribute("currentStatus", filter);
        model.addAttribute("currentSortType", sort);
        model.addAttribute("currentKeyword", keyword);

        String encodedKeyword = "";
        if (keyword != null && !keyword.trim().isEmpty()) {
            encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        }

        return "redirect:/tasks?page=" + page + "&filter=" + filter + "&sort=" + sort + "&keyword=" + encodedKeyword;
    }

    // 3.タスクを削除する
    @PostMapping("/delete-task/{id}")
    public String deleteTask(@PathVariable Long id,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "filter", required = false) String filter,
                             @RequestParam(value = "sort", required = false) String sort,
                             @AuthenticationPrincipal SimpleUserDetails userDetails, // ログインユーザーを取得
                             RedirectAttributes redirectAttributes) {
        // セキュリティブロック：削除対象の所有権をチェック
        Task task = taskService.getTaskById(id);

        if (task == null || !task.getSiteUser().getId().equals(userDetails.getUser().getId())) {
            // 他人の情報、または存在しない情報の場合は削除を拒否
            redirectAttributes.addFlashAttribute("errorMessage", "他ユーザの情報は削除できません。");
        } else {
            // 所有者が一致した場合のみ、安全に削除を実行
            taskService.deleteTask(id);
            redirectAttributes.addFlashAttribute("successMessage", "削除しました。");
        }
        redirectAttributes.addAttribute("page", page);
        if (keyword != null) redirectAttributes.addAttribute("keyword", keyword);
        if (filter != null) redirectAttributes.addAttribute("filter", filter);
        if (sort != null) redirectAttributes.addAttribute("sort", sort);
        return "redirect:/tasks";
    }
    // 4. 編集画面を表示する
    @GetMapping("/tasks/{id}/edit")
    public String showEditForm(
            @PathVariable("id") Long id,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort,
            @AuthenticationPrincipal SimpleUserDetails userDetails, // 1. ログインユーザーを取得
            RedirectAttributes redirectAttributes,                  // 2. フラッシュメッセージ用
            Model model) {

        // タスクを取得
        Task task = taskService.getTaskById(id);

        // 3. 所有権のチェック（タスクが存在し、かつ作成者がログインユーザーと異なる場合）
        if (task == null || !task.getSiteUser().getId().equals(userDetails.getUser().getId())) {
            // エラーメッセージを設定（リダイレクト先の一覧画面で1度だけ表示される）
            redirectAttributes.addFlashAttribute("errorMessage", "他ユーザの情報は編集できません。");

            // 4. 現行のパラメータ（検索・ソート・フィルター・ページ）を維持して一覧へリダイレクト
            redirectAttributes.addAttribute("page", page);
            if (keyword != null) redirectAttributes.addAttribute("keyword", keyword);
            if (filter != null) redirectAttributes.addAttribute("filter", filter);
            if (sort != null) redirectAttributes.addAttribute("sort", sort);

            return "redirect:/tasks"; // 一覧画面のパス（環境に合わせて調整してください）
        }

        // 所有者が一致した場合は通常通り編集画面を表示
        model.addAttribute("task", task);
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
            @AuthenticationPrincipal SimpleUserDetails userDetails,
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
        // セキュリティブロック：DBから変更前の既存タスクを取得して所有権をチェック
        Task existingTask = taskService.getTaskById(id);
        if (existingTask == null || !existingTask.getSiteUser().getId().equals(userDetails.getUser().getId())) {
            // 他人のタスク、または存在しないタスクの場合は拒否して一覧へ戻す
            return "redirect:/tasks?page=" + page + "&filter=" + filter + "&keyword=" + keyword + "&sort=" + sort;
        }

        // 3. 【最重要】送信されてきたtaskオブジェクトに、ログインユーザー情報をセットしてnullを解消
        task.setSiteUser(userDetails.getUser());

        // 必要に応じて、画面から送信されない項目（作成日時など）を既存データから引き継ぐ
        task.setCreatedAt(existingTask.getCreatedAt());

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