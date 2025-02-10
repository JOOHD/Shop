package JOO.jooshop.contentImgs.entity;

import JOO.jooshop.product.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "content_imgs")
@NoArgsConstructor // 디폴트 생성자
public class ContentImages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_img_id")
    private Long contentImgId;

    @Column(name = "image_path", nullable = false)
    private String imagePath;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    // 생성자 추가
    public ContentImages(Product product, String imagePath) {
        this.product = product;
        this.imagePath = imagePath;
        this.createdAt = LocalDateTime.now();

    }
    @Override
    public String toString() {
        return this.getImagePath(); // imagePath를 반환
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
