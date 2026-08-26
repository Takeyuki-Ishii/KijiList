package com.example.KijiList;

import com.example.KijiList.entity.SiteUser;
import com.example.KijiList.service.SimpleUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service // ★Spring Bootに「これはサービス層のクラスです」と教えるアノテーション
public class TaskService {

    private final TaskRepository taskRepository;

    // ★コンストラクタインジェクションでRepositoryを注入
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // タスクを保存・更新する
    public void saveTask(Task task) {
        taskRepository.save(task);
    }

    // IDからタスクを1件取得する
    public Task getTaskById(Long task_id) {
        return taskRepository.findById(task_id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid task Id:" + task_id));
    }

    // タスクを完了状態にする
    public void completeTask(Long id) {
        taskRepository.completeTaskById(id);
    }

    // タスクを削除する
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    // タスクの取得
    public Page<Task> getTasks(@AuthenticationPrincipal SimpleUserDetails userDetails,
                               int page,
                               String status,
                               String keyword,
                               String sortType) {
        int pageSize = 5; // 1ページあたりの件数

        // 完了状態（status）の文字列を、リポジトリが求める Boolean (isCompleted) に変換
        Boolean isCompleted = null;
        if ("completed".equals(status)) {
            isCompleted = true;
        } else if ("uncompleted".equals(status)) {
            isCompleted = false;
        }
        // 2. ログインユーザーの権限を確認
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Repositoryの@Query側でCASE文ソートを行うため、ここではソート条件を指定しない（Sort.unsorted()）
        Pageable pageable = PageRequest.of(page, pageSize, Sort.unsorted());

        Page<Task> taskPage;

        // 3. 権限によるクエリの呼び分け
        if (isAdmin) {
            // 管理者は全件対象のクエリ
            taskPage = taskRepository.findAllByKeywordAndStatusAndSortAsAdmin(keyword, isCompleted, sortType, pageable);
        } else {
            // 一般ユーザーは自分のIDで絞り込み
            taskPage = taskRepository.findByKeywordAndStatusAndSort(userDetails.getUser().getId(), keyword, isCompleted, sortType, pageable);
        }

        // 引数の順番をRepositoryの定義と合わせる
        return taskPage;
    }

    // 既存のクラス内に配置してください
    @Transactional // データベースの更新・削除処理を行うため、トランザクション管理を付与します
    public void deleteTasks(List<Long> taskIds) {
        // Repositoryの一括削除メソッドを呼び出します
        taskRepository.deleteAllByIdInBatch(taskIds);
    }
}
