package com.example.KijiList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // 全てのタスクを取得する
    public List<Task> getAllTasks() {
        // 以前の findAllByOrderByDeadlineAsc() から変更します
        return taskRepository.findAllPrioritizedTasks();
    }

    // タスクを並び変えて取得する
    public List<Task> getTasks(String sort) {
        if ("priority".equals(sort)) {
            return taskRepository.findAllPrioritizedTasks();
        } else {
            return taskRepository.findAllByOrderByDeadlineAsc();
        }
    }
    public List<Task> getFilteredAndSortedTasks(String filter, String sort) {
        // 1. 「未完了のみ(active)」または「完了のみ(completed)」で絞り込む場合
        if ("active".equals(filter) || "completed".equals(filter)) {
            boolean completedStatus = "completed".equals(filter); // completedならtrue、activeならfalse

            if ("priority".equals(sort)) {
                return taskRepository.findAllByCompletedPrioritized(completedStatus);
            } else {
                return taskRepository.findAllByCompletedOrderByDeadlineAsc(completedStatus);
            }
        }

        // 2. 「すべて(all)」の場合は、前回のソートロジックをそのまま使う
        if ("priority".equals(sort)) {
            return taskRepository.findAllPrioritizedTasks();
        } else {
            return taskRepository.findAllByOrderByDeadlineAsc();
        }
    }
    // タスクを保存・更新する
    public void saveTask(Task task) {
        taskRepository.save(task);
    }

    // IDからタスクを1件取得する
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid task Id:" + id));
    }

    // タスクを完了状態にする
    public void completeTask(Long id) {
        Task task = getTaskById(id);
        task.setCompleted(true);
        taskRepository.save(task);
    }

    // タスクを削除する
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    // タスクの取得
    public Page<Task> getTasks(int page, String status, String keyword, String sortType) {
        int pageSize = 5; // 1ページあたりの件数

        // 完了状態（status）の文字列を、リポジトリが求める Boolean (isCompleted) に変換
        Boolean isCompleted = null;
        if ("completed".equals(status)) {
            isCompleted = true;
        } else if ("uncompleted".equals(status)) {
            isCompleted = false;
        }

        // Repositoryの@Query側でCASE文ソートを行うため、ここではソート条件を指定しない（Sort.unsorted()）
        Pageable pageable = PageRequest.of(page, pageSize, Sort.unsorted());

        // 引数の順番をRepositoryの定義と合わせる
        return taskRepository.findByKeywordAndStatusAndSort(keyword, isCompleted, sortType, pageable);
    }

    // 既存のクラス内に配置してください
    @Transactional // データベースの更新・削除処理を行うため、トランザクション管理を付与します
    public void deleteTasks(List<Long> taskIds) {
        // Repositoryの一括削除メソッドを呼び出します
        taskRepository.deleteAllByIdInBatch(taskIds);
    }
}
