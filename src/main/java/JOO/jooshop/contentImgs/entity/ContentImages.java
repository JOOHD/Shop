package JOO.jooshop.contentImgs.entity;

import JOO.jooshop.contentImgs.entity.enums.UploadType;
import JOO.jooshop.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "content_imgs", indexes = {
        @Index(name = "idx_content_imgs_product", columnList = "product_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentImages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_img_id")
    private Long contentImgId;

    @Column(name = "image_path", nullable = false, length = 2000)
    private String imagePath;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_type", nullable = false, length = 30)
    private UploadType uploadType;

    // ✅ createdAt은 DB/하이버네이트에 위임 (한 방식으로 통일)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* ========= Factory ========= */

    public static ContentImages create(Product product, String imagePath, UploadType uploadType) {
        if (product == null) throw new IllegalArgumentException("product must not be null");
        String normalized = normalizePath(imagePath);
        if (normalized == null) throw new IllegalArgumentException("imagePath is invalid");
        if (uploadType == null) throw new IllegalArgumentException("uploadType must not be null");

        ContentImages img = new ContentImages();
        img.product = product;
        img.imagePath = normalized;
        img.uploadType = uploadType;
        return img;
    }

    /* ========= Domain methods ========= */

    /** 필요 시 경로 교체(운영 정책상 금지라면 삭제해도 됨) */
    public void changePath(String newPath) {
        String normalized = normalizePath(newPath);
        if (normalized == null) throw new IllegalArgumentException("imagePath is invalid");
        this.imagePath = normalized;
    }

    /* ========= Helpers ========= */

    private static String normalizePath(String path) {
        if (path == null) return null;
        String t = path.trim();
        if (t.isBlank()) return null;
        return t;
    }

    @Override
    public String toString() {
        // ✅ 엔티티 toString에서 path 노출은 로그 오염/보안/길이 문제 생김
        return "ContentImages{id=" + contentImgId + ", uploadType=" + uploadType + "}";
    }
}
