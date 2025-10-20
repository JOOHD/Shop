package JOO.jooshop.thumbnail.entity;

import JOO.jooshop.product.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor // 디폴트 생성자
@Table(name = "product_thumbnails")
public class ProductThumbnail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "thumbnail_id")
    private Long thumbnailId;   // PK

    @Column(name = "image_path", nullable = false)
    private String imagePath;   // 사진 경로

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;    // FK

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    // 생성자 추가
    public ProductThumbnail(Product product, String imagePath) {
        this.product = product;
        this.imagePath = imagePath;
        this.createdAt = LocalDateTime.now();

    }
}