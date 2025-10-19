package JOO.jooshop.contentImgs.entity;

import JOO.jooshop.contentImgs.entity.enums.UploadType;
import JOO.jooshop.product.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "content_imgs")
@NoArgsConstructor
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

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_type", nullable = false)
    private UploadType uploadType;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    public ContentImages(Product product, String imagePath, UploadType uploadType) {
        this.product = product;
        this.imagePath = imagePath;
        this.uploadType = uploadType;
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return this.getImagePath();
    }
}
