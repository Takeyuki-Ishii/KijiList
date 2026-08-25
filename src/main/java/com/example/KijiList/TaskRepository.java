package com.example.KijiList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> { // ★★★ これを追加 ★★★

    // キーワード、完了状態、ソート条件、およびログインユーザーで絞り込むカスタムクエリ
    @Query("SELECT t FROM Task t WHERE " +
            "t.siteUser.id = :userId AND " + // 1. ログインユーザーのIDで絞り込み（必須条件）
            "(:keyword IS NULL OR :keyword = '' OR LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:isCompleted IS NULL OR t.completed = :isCompleted) " +
            "ORDER BY " +
            "CASE WHEN LOWER(:sort) = 'priority' THEN " +
            "  CASE t.priority WHEN '高' THEN 1 WHEN '中' THEN 2 WHEN '低' THEN 3 ELSE 4 END " +
            "ELSE 0 END ASC, " +
            "t.deadline ASC")
    Page<Task> findByKeywordAndStatusAndSort(
            @Param("userId") Long userId, // 2. 引数にuserIdを追加
            @Param("keyword") String keyword,
            @Param("isCompleted") Boolean isCompleted,
            @Param("sort") String sort,
            Pageable pageable);

    @Transactional
    @Modifying
    @Query("UPDATE Task t SET t.completed = true WHERE t.id = :id")
    void completeTaskById(@Param("id") Long id);
}
