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
@Table(
        name = "content_imgs",
        indexes = {
                @Index(name = "idx_content_imgs_product", columnList = "product_id")
        }
)
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* =========================================================
       Factory
    ========================================================= */

    public static ContentImages create(Product product, String imagePath, UploadType uploadType) {
        if (product == null) throw new IllegalArgumentException("product must not be null");

        String normalized = normalizePath(imagePath);
        if (normalized == null) throw new IllegalArgumentException("imagePath is invalid");
        if (uploadType == null) throw new IllegalArgumentException("uploadType must not be null");

        ContentImages img = new ContentImages();
        img.attachTo(product);
        img.imagePath = normalized;
        img.uploadType = uploadType;
        return img;
    }

    /* =========================================================
       Association (attach / detach)
       - 연관관계 세팅은 여기로 통일
    ========================================================= */

    public void attachTo(Product product) {
        if (product == null) throw new IllegalArgumentException("product must not be null");
        this.product = product;
    }

    public void detach() {
        this.product = null;
    }

    /* =========================================================
       Domain methods
    ========================================================= */

    /** 필요 시 경로 교체(운영 정책상 금지라면 삭제해도 됨) */
    public void changePath(String newPath) {
        String normalized = normalizePath(newPath);
        if (normalized == null) throw new IllegalArgumentException("imagePath is invalid");
        this.imagePath = normalized;
    }

    /* =========================================================
       Helpers
    ========================================================= */

    private static String normalizePath(String path) {
        if (path == null) return null;
        String t = path.trim();
        if (t.isBlank()) return null;
        return t;
    }

    @Override
    public String toString() {
        return "ContentImages{id=" + contentImgId + ", uploadType=" + uploadType + "}";
    }
}
