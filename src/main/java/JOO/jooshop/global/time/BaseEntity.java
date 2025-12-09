package JOO.jooshop.global.time;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass // 공통 필드 상속용, 테이블은 안 만듦
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * 특정 이벤트가 발생할 때(예: insert, update) 자동으로 특정 로직 수행 가능
     *
     * Spring Data JPA Auditing과 함께 쓰면:
     * @CreatedDate → insert 시점 자동 채움
     * @LastModifiedDate → update 시점 자동 채움
     *
     *  기존 nullable = false 제거, 
     *  컬럼이 NULL 허용 -> 기존 row 저장 가능 -> 이후 신규 데이터는 @PrePersist 로 자동으로 채워짐
     */
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    /** 엔티티 수정 시점 (UPDATE 시 자동 세팅) */
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}


