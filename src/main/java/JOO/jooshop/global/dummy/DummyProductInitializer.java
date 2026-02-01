package JOO.jooshop.global.dummy;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.Gender;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.entity.enums.Size;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productManagement.repository.ProductManagementRepository;
import JOO.jooshop.thumbnail.entity.ProductThumbnail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Slf4j
@Profile("local") // local 환경에서만 실행
@Component
@RequiredArgsConstructor
public class DummyProductInitializer implements CommandLineRunner {

    /**
     * 클래스 목적
     * 로컬 환경에서 테스트용으로 더미 상품 데이터를 DB에 넣고, Admin 화면에서 CRUD 및 상품 리스트 확인
     * -> 검증 목적이 아닌, 화면 테스트 및 초기 데이터 제공 목적 (entity + repository)
     *

     [DummyProductInitializer]
            `↓ (앱 시작 시 자동 실행)
     Product + ProductThumbnail(imagePath = "https://...") 저장
            ↓
     DB에는 그냥 문자열(URL)만 있음
            ↓
     [관리자 상품 목록 요청]
            ↓
     Controller → Service → Repository
            ↓
     Product + ProductThumbnail 조회
            ↓
     DTO or 엔티티가 model에 담김
            ↓
     Thymeleaf가 imagePath를 <img src="..."> 로 렌더링
            ↓
     브라우저가 "https://store.manutd.com/..." 로 직접 이미지 요청

     * 
     * 핵심 포인트 12/20
     * - 외부 이미지 URL 사용: 로컬에 이미지 저장 없이 URL로 바로 렌더링
     * - Controller에 더미 데이터 넣지 않음: DB 초기화용, DummyProductInitializer 로 분리
     * - DTO 사용: 화면용 데이터 (AdminProductResponseDto) 로 변환
     * - Thymeleaf 와 연동: productList.html 에서 DTO 기반 반복 렌더링
     *
     * 리팩토링 12/21
     * 1. @Transactional 제거
     *   - 더미 초기화 시, validation / flush 에러를 즉시 확인하기 위해
     *   - CommandLineRunner 에서는 트랜잭션을 제거했다.
     * 2. Thumbnail 옵션 루트 밖에서 생성
     *   - 썸네일은 상품 대표 이미지 개념이기 때문에 PM 과 독립적으로 1회만 생성 되도록 분리
     */

    private final ProductRepositoryV1 productRepository;
    private final ProductManagementRepository productManagementRepository;
    private final Random random = new Random();

    @Override
    public void run(String... args) {
        createDummyProducts();
    }

    private void createDummyProducts() {
        // 상품명 + 외부 이미지 URL
        List<String> productNames = List.of(
                "2025 맨유 홈 저지",
                "2025 맨유 어웨이 저지",
                "2025 맨유 서드 저지",
                "2025 맨유 트레이닝 웨어",
                "2025 맨유 롱슬리브",
                "맨유 키즈 홈 저지",
                "맨유 액세서리 모자",
                "맨유 액세서리 스카프",
                "맨유 AWAY 긴팔 티",
                "맨유 THIRD 반팔 티"
        );

        List<String> imageUrls = List.of(
                "https://store.manutd.com/media/catalog/product/cache/8f0c8c95c03f3dc1615e62d6b91e2b36/m/u/mufc_2025_home_jersey.jpg",
                "https://store.manutd.com/media/catalog/product/cache/8f0c8c95c03f3dc1615e62d6b91e2b36/m/u/mufc_2025_away_jersey.jpg",
                "https://store.manutd.com/media/catalog/product/cache/8f0c8c95c03f3dc1615e62d6b91e2b36/m/u/mufc_2025_third_jersey.jpg",
                "https://store.manutd.com/media/catalog/product/cache/8f0c8c95c03f3dc1615e62d6b91e2b36/m/u/mufc_2025_long_sleeve.jpg",
                "https://store.manutd.com/media/catalog/product/cache/8f0c8c95c03f3dc1615e62d6b91e2b36/t/r/training_wear.jpg",
                "https://store.manutd.com/media/catalog/product/cache/8f0c8c95c03f3dc1615e62d6b91e2b36/m/u/mufc_scarf.jpg",
                "https://store.manutd.com/media/catalog/product/cache/8f0c8c95c03f3dc1615e62d6b91e2b36/m/u/mufc_hat.jpg",
                "https://store.manutd.com/media/catalog/product/cache/8f0c8c95c03f3dc1615e62d6b91e2b36/m/u/mufc_kit.jpg",
                "https://store.manutd.com/media/catalog/product/cache/8f0c8c95c03f3dc1615e62d6b91e2b36/m/u/mufc_home_gloves.jpg",
                "https://store.manutd.com/media/catalog/product/cache/8f0c8c95c03f3dc1615e62d6b91e2b36/m/u/mufc_boots.jpg"
        );

        for (int i = 0; i < productNames.size(); i++) {
            try {
                Product product = createProduct(productNames.get(i));
                addThumbnail(product, imageUrls.get(i));
                productRepository.save(product); // cascde 로 thumbnail 함께 저장

                createOptions(product);

                log.info("Dummy product created: {}", product.getProductName());
            } catch (Exception e) {
                log.error("Dummy product failed: {}", productNames.get(i), e);
            }
        }
    }

    /** Product 생성 */
    private Product createProduct(String productName){
        return Product.createDummy(
                productName,
                ProductType.values()[random.nextInt(ProductType.values().length)],
                BigDecimal.valueOf(100_000 + random.nextInt(200_000)),
                productName + " 상세 정보",
                "MANUTD Official",
                true,             // isDiscount
                random.nextInt(50),  // discountRate
                true
        );
    }

    /** Thumbnail 1회만 생성 */
    private void addThumbnail(Product product, String imageUrl) {
        ProductThumbnail thumbnail = new ProductThumbnail(product, imageUrl);
        product.addThumbnail(thumbnail);
    }

    /** 옵션 생성 */
    private void createOptions(Product product) {
        for (Gender gender : Gender.values()) {
            for (Size size : Size.values()) {
                ProductManagement pm = ProductManagement.of(
                        product,
                        null,
                        null,
                        size,
                        20L,
                        true,
                        false,
                        false
                );
                pm.setGender(gender);
                productManagementRepository.save(pm);
            }
        }
    }
}

