package com.example.KijiList;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> { // ★★★ これを追加 ★★★
    // 期限（deadline）の昇順（Ascending）で全件取得するメソッド
    List<Task> findAllByOrderByDeadlineAsc();
}
