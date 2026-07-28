package com.example.KijiList;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> { // ★★★ これを追加 ★★★
    // 1. 期限順（以前実装したもの）
    List<Task> findAllByOrderByDeadlineAsc();

    // 2. 重要度順（前回実装したもの）
    @Query("SELECT t FROM Task t ORDER BY " +
            "CASE t.priority WHEN '高' THEN 1 WHEN '中' THEN 2 WHEN '低' THEN 3 ELSE 4 END ASC, " +
            "t.deadline ASC")
    List<Task> findAllPrioritizedTasks();
}
