package JOO.jooshop.product.repository;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepositoryV1 extends JpaRepository<Product, Long> {

    // 상품, 썸네일 조회
    @Query("SELECT p FROM Product p JOIN FETCH p.productThumbnails")
    List<Product> findAllWithThumbnails();

    // 상품 조회
    Optional<Product> findByProductId(Long productId);

    // 생성일 기준, 내림차순 상품 조회
    Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 찜한 상품 수 기준, 내림차순 상품 조회
    Page<Product> findAllByOrderByWishListCountDesc(Pageable pageable); // 위시(찜) 많은 순

    // 할인 중인 상품 조회
    Page<Product> findByIsDiscountTrue(Pageable pageable); // 할인목록

    // 추천 상품 조회
    Page<Product> findByIsRecommendTrue(Pageable pageable); // 추천목록

    // 특정 타입 상품 조회
    Page<Product> findByProductType(ProductType productType, Pageable pageable);

    // 특정 날짜 이후에 생성된 상품 조회 (생성일 기준 내림차순)
    Page<Product> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date, Pageable pageable);

    // 찜한 상품 수가 특정 수치 이상인 상품조회
    Page<Product> findByWishListCountGreaterThanOrderByWishListCountDesc(int count, Pageable pageable);
}
