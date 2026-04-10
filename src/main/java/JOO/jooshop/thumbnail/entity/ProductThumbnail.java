package JOO.jooshop.thumbnail.entity;

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
        name = "product_thumbnails",
        indexes = {
                @Index(name = "idx_thumbnails_product", columnList = "product_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductThumbnail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "thumbnail_id")
    private Long thumbnailId;

    @Column(name = "image_path", nullable = false, length = 2000)
    private String imagePath;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* =========================================================
       Factor
        - Product 와의 연결은 여기서 하지 않는다.
    ========================================================= */

    public static ProductThumbnail createThumbnail(String imagePath) {
        String normalized = normalizePath(imagePath);
        if (normalized == null) {
            throw new IllegalArgumentException("imagePath is invalid");
        }

        ProductThumbnail thumbnail = new ProductThumbnail();
        thumbnail.imagePath = normalized;
        return thumbnail;
    }

    /* =========================================================
       Association (attach / detach)
       - 실제 흐름은 Product가 담당
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

    public void changePath(String newPath) {
        String normalized = normalizePath(newPath);
        if (normalized == null) throw new IllegalArgumentException("imagePath is invalid");
        this.imagePath = normalized;
    }

    private static String normalizePath(String path) {
        if (path == null) return null;
        String t = path.trim();
        if (t.isBlank()) return null;
        return t;
    }

    @Override
    public String toString() {
        return "ProductThumbnail{id=" + thumbnailId + "}";
    }
}
