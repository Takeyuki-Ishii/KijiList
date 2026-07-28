package com.example.KijiList;

import org.springframework.stereotype.Service;
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
}
