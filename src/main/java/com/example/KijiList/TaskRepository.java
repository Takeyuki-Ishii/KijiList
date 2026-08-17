package com.example.KijiList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> { // ★★★ これを追加 ★★★
    // 1. 期限順
    List<Task> findAllByOrderByDeadlineAsc();

    // 2. 重要度順
    @Query("SELECT t FROM Task t ORDER BY " +
            "CASE t.priority WHEN '高' THEN 1 WHEN '中' THEN 2 WHEN '低' THEN 3 ELSE 4 END ASC, " +
            "t.deadline ASC")
    List<Task> findAllPrioritizedTasks();

    // 3. 完了状態で絞り込み ➔ 期限順
    List<Task> findAllByCompletedOrderByDeadlineAsc(boolean completed);

    // 4. 完了状態で絞り込み ➔ 重要度順（CASE文に「WHERE t.completed = :completed」を挟みます）
    @Query("SELECT t FROM Task t WHERE t.completed = :completed ORDER BY " +
            "CASE t.priority WHEN '高' THEN 1 WHEN '中' THEN 2 WHEN '低' THEN 3 ELSE 4 END ASC, " +
            "t.deadline ASC")
    List<Task> findAllByCompletedPrioritized(boolean completed);

    // キーワード、完了状態、ソート条件をすべて網羅するカスタムクエリ
    // 修正案：大文字小文字を区別せず小文字に統一して判定する
    // 戻り値を Page<Task> にし、引数の末尾に Pageable pageable を追加します
    @Query("SELECT t FROM Task t WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:isCompleted IS NULL OR t.completed = :isCompleted) " +
            "ORDER BY " +
            "CASE WHEN LOWER(:sort) = 'priority' THEN " +
            "  CASE t.priority WHEN '高' THEN 1 WHEN '中' THEN 2 WHEN '低' THEN 3 ELSE 4 END " +
            "ELSE 0 END ASC, " +
            "t.deadline ASC")
    Page<Task> findByKeywordAndStatusAndSort(
            @Param("keyword") String keyword,
            @Param("isCompleted") Boolean isCompleted,
            @Param("sort") String sort,
            Pageable pageable);
}
