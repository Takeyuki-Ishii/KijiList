package com.example.KijiList;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

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
            @RequestParam(value = "status", defaultValue = "all") String status,
            @RequestParam(value = "sort", defaultValue = "deadline") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @AuthenticationPrincipal SimpleUserDetails userDetails, // ★追加：ログインユーザーの取得
            Model model) {

        Page<Task> taskPage = taskService.getTasks(userDetails, page, status, keyword, sort);

        // Thymeleafに渡すデータをモデルに格納
        model.addAttribute("taskPage", taskPage);                // Pageオブジェクトごと渡す
        model.addAttribute("taskList", taskPage.getContent());    // ループ用（現在のページのタスクリスト）

        // 現在の条件をURLパラメータ維持のためにモデルに保持
        model.addAttribute("currentPage", page);
        model.addAttribute("currentStatus", status);
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
            @RequestParam(defaultValue = "all") String status, // 大文字小文字のズレを防ぐため小文字推奨（コントローラーのデフォルト値に合わせる）
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "deadline") String sort,
            @AuthenticationPrincipal SimpleUserDetails userDetails)// ★追加：ログインユーザーの取得
    {
        // ★ログインユーザーのEntityを取得
        SiteUser loginUser = userDetails.getUser();

        // 1. バリデーションエラーを検知した場合
        if (bindingResult.hasErrors()) {

            // ★修正：一覧データの再取得時にも loginUser を渡す（4連動を維持するため）
            Page<Task> taskPage = taskService.getTasks(userDetails, page, status, keyword, sort);
            model.addAttribute("taskPage", taskPage);
            model.addAttribute("taskList", taskPage.getContent());

            // URLパラメータ維持のためのデータも詰め直す
            model.addAttribute("currentPage", page);
            model.addAttribute("currentStatus", status);
            model.addAttribute("currentSortType", sort);
            model.addAttribute("currentKeyword", keyword);

            return "task-list";
        }

        // 2. エラーがなければ正常に保存してリダイレクト
        // ★重要：画面から送られてきたタスクオブジェクトに、現在のログインユーザーを紐付ける
        task.setSiteUser(loginUser);

        taskService.saveTask(task);

        model.addAttribute("currentPage", page);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentSortType", sort);
        model.addAttribute("currentKeyword", keyword);

        String encodedKeyword = "";
        if (keyword != null && !keyword.trim().isEmpty()) {
            encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        }

        return "redirect:/tasks?page=" + page + "&status=" + status + "&sort=" + sort + "&keyword=" + encodedKeyword;
    }

    // 3.タスクを削除する
    @PostMapping("/delete-task/{id}")
    public String deleteTask(@PathVariable Long id,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "status", required = false) String status,
                             @RequestParam(value = "sort", required = false) String sort,
                             @AuthenticationPrincipal SimpleUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {

        Task task = taskService.getTaskById(id);
        if (task == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "該当の情報が見つかりません。");
            redirectAttributes.addAttribute("page", page);
            if (keyword != null) redirectAttributes.addAttribute("keyword", keyword);
            if (status != null) redirectAttributes.addAttribute("status", status);
            if (sort != null) redirectAttributes.addAttribute("sort", sort);
            return "redirect:/tasks";
        }

        // 1. 管理者チェック
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        // 2. 所有者チェック（Objects.equals で安全に比較して警告を解消）
        boolean isOwner = Objects.equals(task.getSiteUser().getId(), userDetails.getUser().getId());

        if (!isAdmin && !isOwner) {
            // 他人の情報、または存在しない情報の場合は削除を拒否
            redirectAttributes.addFlashAttribute("errorMessage", "他ユーザの情報は削除できません。");
        } else {
            // 管理者、または所有者が一致した場合のみ、安全に削除を実行
            taskService.deleteTask(id);
            redirectAttributes.addFlashAttribute("successMessage", "削除しました。");
        }

        long remainingCount = taskService.countTasksByCondition(userDetails.getUser().getId(), keyword, status);
        int pageSize = 5; // 1ページ5件ずつの仕様

        // 最大ページ数（0開始）を計算。タスクが0件なら最大ページは0。
        int maxPage = (remainingCount == 0) ? 0 : (int) ((remainingCount - 1) / pageSize);

        // 要求されたページが、削除後の最大ページ数を超えていたら上書きする
        if (page > maxPage) {
            page = maxPage;
        }

        // パラメータの維持ロジック
        redirectAttributes.addAttribute("page", page);
        if (keyword != null) redirectAttributes.addAttribute("keyword", keyword);
        if (status != null) redirectAttributes.addAttribute("status", status);
        if (sort != null) redirectAttributes.addAttribute("sort", sort);
        return "redirect:/tasks";
    }
    // 4. 編集画面を表示する
    @GetMapping("/tasks/{id}/edit")
    public String showEditForm(
            @PathVariable("id") Long id,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "sort", required = false) String sort,
            @AuthenticationPrincipal SimpleUserDetails userDetails, // 1. ログインユーザーを取得
            RedirectAttributes redirectAttributes,                  // 2. フラッシュメッセージ用
            Model model) {

        // タスクを取得
        Task task = taskService.getTaskById(id);
        if (task == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "該当の情報が見つかりません。");
            redirectAttributes.addAttribute("page", page);
            if (keyword != null) redirectAttributes.addAttribute("keyword", keyword);
            if (status != null) redirectAttributes.addAttribute("status", status);
            if (sort != null) redirectAttributes.addAttribute("sort", sort);
            return "redirect:/tasks";
        }

        // 1. 管理者チェック
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        // 2. 所有者チェック（Objects.equals で安全に比較して警告を解消）
        boolean isOwner = Objects.equals(task.getSiteUser().getId(), userDetails.getUser().getId());

        if (!isAdmin && !isOwner) {
        // 3. 所有権のチェック（タスクが存在し、かつ作成者がログインユーザーと異なる場合）
            // エラーメッセージを設定（リダイレクト先の一覧画面で1度だけ表示される）
            redirectAttributes.addFlashAttribute("errorMessage", "他ユーザの情報は編集できません。");

            // 4. 現行のパラメータ（検索・ソート・フィルター・ページ）を維持して一覧へリダイレクト
            redirectAttributes.addAttribute("page", page);
            if (keyword != null) redirectAttributes.addAttribute("keyword", keyword);
            if (status != null) redirectAttributes.addAttribute("status", status);
            if (sort != null) redirectAttributes.addAttribute("sort", sort);

            return "redirect:/tasks"; // 一覧画面のパス（環境に合わせて調整してください）
        }

        // 所有者が一致した場合は通常通り編集画面を表示
        model.addAttribute("task", task);
        model.addAttribute("currentPage", page);
        model.addAttribute("returnKeyword", keyword);
        model.addAttribute("returnStatus", status);
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
            @RequestParam(defaultValue = "all") String status,
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
            model.addAttribute("currentStatus", status);
            model.addAttribute("currentSortType", sort);
            model.addAttribute("currentKeyword", keyword);

            // データベースへは保存（update）せず、そのまま編集画面（task-edit）を呼び出して再表示する
            return "task-edit";
        }

        // セキュリティブロック：DBから変更前の既存タスクを取得して所有権をチェック
        Task existingTask = taskService.getTaskById(id);
        if (existingTask == null) {
            return "redirect:/tasks?page=" + page + "&status=" + status + "&keyword=" + keyword + "&sort=" + sort;
        }
        // 1. 管理者チェック
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        // 2. 所有者チェック（Objects.equals で安全に比較して警告を解消）
        boolean isOwner = Objects.equals(existingTask.getSiteUser().getId(), userDetails.getUser().getId());

        if (!isAdmin && !isOwner) {
            // 他人のタスク、または存在しないタスクの場合は拒否して一覧へ戻す
            return "redirect:/tasks?page=" + page + "&status=" + status + "&keyword=" + keyword + "&sort=" + sort;
        }

        // 3. 【最重要】送信されてきたtaskオブジェクトに、ログインユーザー情報をセットしてnullを解消
        task.setSiteUser(existingTask.getSiteUser());

        // 必要に応じて、画面から送信されない項目（作成日時など）を既存データから引き継ぐ
        task.setCreatedAt(existingTask.getCreatedAt());
        // エラーがなければ正常に更新処理を行ってリダイレクト
        taskService.saveTask(task);
        String encodedKeyword = "";
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 「作業」を「%E4%BD%9C%E6%A5%AD」のような形式に安全に変換します
            encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        }
        // リダイレクト先にも現在のページや検索条件を引き継ぐ
        return "redirect:/tasks?page=" + page + "&status=" + status + "&sort=" + sort + "&keyword=" + encodedKeyword;
    }

    // 6.完了フラグをONにする
    @PostMapping("/complete-task/{id}")
    public String completeTask(@PathVariable Long id,
                               @RequestParam(value = "page", required = false) int page,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "status", required = false) String status,
                               @RequestParam(value = "sort", required = false) String sort,
                               @AuthenticationPrincipal SimpleUserDetails userDetails, // 認証情報の取得
                               RedirectAttributes redirectAttributes) {

        // 状態維持パラメータを事前にセット
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("keyword", keyword);
        redirectAttributes.addAttribute("status", status);
        redirectAttributes.addAttribute("sort", sort);

        // セキュリティブロック：DBから変更前の既存タスクを取得して所有権をチェック
        Task existingTask = taskService.getTaskById(id);
        if (existingTask == null) {
            return "redirect:/tasks?page=" + page + "&status=" + status + "&keyword=" + keyword + "&sort=" + sort;
        }
        // 1. 管理者チェック
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        // 2. 所有者チェック（Objects.equals で安全に比較して警告を解消）
        boolean isOwner = Objects.equals(existingTask.getSiteUser().getId(), userDetails.getUser().getId());


        // 「管理者である」または「タスクの所有者である」のどちらも満たさない場合はブロック
        if (!isAdmin && !isOwner) {
            redirectAttributes.addFlashAttribute("errorMessage", "他人のタスクを完了にすることはできません。");
            return "redirect:/tasks";
        }

        // 完了フラグ更新処理
        taskService.completeTask(id);
        redirectAttributes.addFlashAttribute("successMessage", "ステータスを完了にしました。");

        long remainingCount = taskService.countTasksByCondition(userDetails.getUser().getId(), keyword, status);
        int pageSize = 5; // 1ページ5件ずつの仕様

        // 最大ページ数（0開始）を計算。タスクが0件なら最大ページは0。
        int maxPage = (remainingCount == 0) ? 0 : (int) ((remainingCount - 1) / pageSize);

        // 要求されたページが、削除後の最大ページ数を超えていたら上書きする
        if (page > maxPage) {
            page = maxPage;
        }

        // 🌟【修正】確定した安全なpage数をリダイレクト属性にセットする
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("keyword", keyword);
        redirectAttributes.addAttribute("status", status);
        redirectAttributes.addAttribute("sort", sort);
        return "redirect:/tasks";
    }
    // 7.タスクの一括削除
    @PostMapping("/tasks/bulk-delete")
    public String bulkDeleteTasks(
            @RequestParam(value = "taskIds", required = false) List<Long> taskIds,
            @RequestParam(value = "page", required = false) int page,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", defaultValue = "ALL") String status,
            @RequestParam(value = "sort", defaultValue = "deadline") String sort,
            @AuthenticationPrincipal SimpleUserDetails userDetails, // 認証情報の取得
            RedirectAttributes redirectAttributes) {

        // 状態維持パラメータを事前にセット（RedirectAttributesが自動でURLエンコードを行うため手動エンコードは不要）
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("keyword", keyword);
        redirectAttributes.addAttribute("status", status);
        redirectAttributes.addAttribute("sort", sort);

        // 1. チェックボックスが選択されているか確認
        if (taskIds == null || taskIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "削除するタスクが選択されていません。");
            return "redirect:/tasks";
        }

        // 2. 権限チェック（管理者かどうかの判定）
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        // 3. 各タスクの所有権を全件チェック
        for (Long id : taskIds) {
            Task existingTask = taskService.getTaskById(id);
            // タスクが存在しない場合はスキップ、またはエラーハンドリング
            if (existingTask == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "存在しないタスクが選択されています。");
                return "redirect:/tasks";
            }

            // 所有者チェック
            boolean isOwner = Objects.equals(existingTask.getSiteUser().getId(), userDetails.getUser().getId());

            // 管理者でもなく、かつ所有者でもないタスクが1件でも混ざっていれば即ブロック
            if (!isAdmin && !isOwner) {
                redirectAttributes.addFlashAttribute("errorMessage", "他人のタスクを一括削除することはできません。");
                return "redirect:/tasks";
            }
        }

        // 4. すべてのタスクの権限チェックを通過した場合のみ削除処理を実行
        taskService.deleteTasks(taskIds);
        redirectAttributes.addFlashAttribute("successMessage", taskIds.size() + "件のタスクを一括削除しました。");

        // 【追加】削除後の残りタスク件数に基づいてページ数を補正する
        // ※ service側のメソッド名や引数は、お使いの環境（キーワードやフィルターの考慮状況）に合わせて調整してください。
        long remainingCount = taskService.countTasksByCondition(userDetails.getUser().getId(), keyword, status);
        int pageSize = 5; // 1ページ5件ずつの仕様

        // 最大ページ数（0開始）を計算。タスクが0件なら最大ページは0。
        int maxPage = (remainingCount == 0) ? 0 : (int) ((remainingCount - 1) / pageSize);

        // 要求されたページが、削除後の最大ページ数を超えていたら上書きする
        if (page > maxPage) {
            page = maxPage;
        }

        // 🌟【修正】確定した安全なpage数をリダイレクト属性にセットする
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("keyword", keyword);
        redirectAttributes.addAttribute("status", status);
        redirectAttributes.addAttribute("sort", sort);

        return "redirect:/tasks";
    }
}