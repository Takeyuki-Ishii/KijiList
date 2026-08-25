package com.example.KijiList;

import com.example.KijiList.entity.SiteUser;
import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // タイトル：空文字やスペースのみを禁止する
    @NotBlank(message = "タイトルを入力してください")
    @Size(max = 50, message = "タイトルは50文字以内で入力してください")
    private String name;

    // ★期限：StringからLocalDateに変更し、未入力禁止 ＋ 今日以降のみ許可
    @NotNull(message = "期限を入力してください")
    @FutureOrPresent(message = "期限には今日以降の日付を指定してください")
    private LocalDate deadline;

    // 完了フラグ（初期値は未完了の false）
    private boolean completed = false;

    // 重要度（"高", "中", "低" のいずれかの文字列が入る）
    private String priority;

    @Column(columnDefinition = "TEXT") // 長文に対応させる設定
    @Size(max = 100, message = "メモは100文字以内で入力してください")
    private String memo;

    @CreatedDate // 作成時に自動挿入
    @Column(updatable = false, nullable = false) // 更新時には上書きさせない
    private LocalDateTime createdAt;

    @LastModifiedDate // 更新時に自動挿入
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ★ ユーザーとの紐付け（多対一のリレーション）
    @ManyToOne(fetch = FetchType.LAZY) // 必要な時だけユーザー情報を読み込む設定（パフォーマンス向上）
    @JoinColumn(name = "user_id", nullable = false) // DB側に「user_id」という外部キーカラムを作成
    private SiteUser siteUser;

    public Task() {
    }

    // ★コンストラクタの引数もLocalDateに変更
    public Task(String name, LocalDate deadline) {
        this.name = name;
        this.deadline = deadline;
    }
    // ゲッター・セッター
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public boolean isCompleted() {
        return completed;
    }
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getPriority() {
        return priority;
    }
    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public SiteUser getSiteUser() {
        return siteUser;
    }
    public void setSiteUser(SiteUser siteUser) {
        this.siteUser = siteUser;
    }
    // ★ 動的に期限切れを判定するメソッド
    public boolean isOverdue() {
        // 未完了、かつ、期限が今日よりも前の場合にtrue
        return !this.completed && this.deadline != null && this.deadline.isBefore(LocalDate.now());
    }
}