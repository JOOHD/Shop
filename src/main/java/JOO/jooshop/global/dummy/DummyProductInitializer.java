package JOO.jooshop.global.dummy;

import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.categorys.repository.CategoryRepository;
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
     * - 로컬 환경에서만 더미 상품 데이터 초기화
     * - 기존 더미 데이터 제거 후 재삽입
     * - 외부 이미지 URL 기반 썸네일 테스트
     * - Admin 상품 리스트 / 썸네일 렌더링 검증
     */

     /** flow
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
    */

     /** 리팩토링
     * 핵심 포인트 25 12/20
     * - 외부 이미지 URL 사용: 로컬에 이미지 저장 없이 URL로 바로 렌더링
     * - Controller에 더미 데이터 넣지 않음: DB 초기화용, DummyProductInitializer 로 분리
     * - DTO 사용: 화면용 데이터 (AdminProductResponseDto) 로 변환
     * - Thymeleaf 와 연동: productList.html 에서 DTO 기반 반복 렌더링
     *
     * 리팩토링 25 12/21
     * 1. @Transactional 제거
     *   - 더미 초기화 시, validation / flush 에러를 즉시 확인하기 위해
     *   - CommandLineRunner 에서는 트랜잭션을 제거했다.
     * 2. Thumbnail 옵션 루트 밖에서 생성
     *   - 썸네일은 상품 대표 이미지 개념이기 때문에 PM 과 독립적으로 1회만 생성 되도록 분리
     *   
     * 리팩톨이 26 02/03
     *  기존 DB에 남아 있던 깨진 imagePath 완전 제거
     *  더미 재실행 시 중복/누적 방지
     *  "" / 공백 / http 아닌 URL 저장 원천 차단
     *  썸네일이 없는 상품도 에러 없이 처리
     *  이후 단계(DTO / Service / HTML) 방어 코드가 의미 있게 작동
     */

    private final CategoryRepository categoryRepository;
    private final ProductRepositoryV1 productRepository;
    private final ProductManagementRepository productManagementRepository;
    
    private final Random random = new Random();

    @Override
    public void run(String... args) {
        log.info("[DummyProductInitializer] START");
        
        resetDummyData();       // 기존 데이터 제거
        createDummyProducts();  // 새 데이터 삽입

        log.info("[DummyProductInitializer] END");
    }

    private Category getDefaultCategory() {
        return categoryRepository.findByName("DUMMY")
                .orElseThrow(() -> new IllegalStateException("Dummy category not found"));
    }

    /**
     * ✅ 더미 데이터 초기화
     * - 옵션 → 상품 순서로 삭제 (FK 안전)
     * - 썸네일은 Product cascade/orphanRemoval 전제
     */
    private void resetDummyData() {
        log.info("[DummyProductInitializer] delete existing dummy data");

        productManagementRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    /* 더미 상품 생성 */
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

        // 검증된 CDN 이미지 URL
        List<String> imageUrls = List.of(
                "https://mufc-live.cdn.scayle.cloud/images/aaf0f931ca9bbbca34f519531b1cc8ff.jpg?width=600",
                "https://mufc-live.cdn.scayle.cloud/images/37253ea8264864e69d9c5dfdd28b8569.jpg?width=600",
                "https://mufc-live.cdn.scayle.cloud/images/4644041696bbde141051021ef2325329.jpg?width=600",
                "https://mufc-live.cdn.scayle.cloud/images/4f8dee4dd3a396ed6ed70d46c07982bd.jpg?width=600",
                "https://mufc-live.cdn.scayle.cloud/images/3b1b1f35b7a3d63c879e9c8d98faedb7.jpg?width=600",
                "https://mufc-live.cdn.scayle.cloud/images/2ef4f47f9fa4f0e5c01a9e60f9a25e2e.jpg?width=600",
                "https://mufc-live.cdn.scayle.cloud/images/9a1b7b13bda76b6d64b3c35f8e1f90c4.jpg?width=600",
                "https://mufc-live.cdn.scayle.cloud/images/8c1d09e79b3e55e4e80f4c7f2df08a2e.jpg?width=600",
                "https://mufc-live.cdn.scayle.cloud/images/6f7a0b7b3e36f12f0b2cbf56c54dcd30.jpg?width=600",
                "https://mufc-live.cdn.scayle.cloud/images/1f8c5f8c0e4a94c7b8b6e6a5bb2c6d9a.jpg?width=600"
        );

        for (int i = 0; i < productNames.size(); i++) {
            try {
                Product product = createProduct(productNames.get(i));
                
                // 썸네일 방어 적용
                addThumbnail(product, imageUrls.get(i));

                productRepository.save(product); // cascde 로 thumbnail 저장
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

    /**
     * ✅ 썸네일 URL 방어
     * - null / blank 제거
     * - http(s) 아닌 값 차단
     */
    private void addThumbnail(Product product, String imageUrl) {
        String normalized = normalizeUrl(imageUrl);

        if (normalized == null) {
            log.warn("[Dummy] skip inavlid thumbnail url. product={}",  product.getProductName());
            return;
        }

        ProductThumbnail thumbnail = new ProductThumbnail(product, imageUrl);
        product.addThumbnail(thumbnail);
    }

    /** URL 정규화 & 검증 */
    private String normalizeUrl(String url) {
        if (url == null) return null;

        String trimmed = url.trim();
        if (trimmed.isBlank()) return null;
        if (!trimmed.startsWith("http")) return null;

        return trimmed;
    }

    /** 옵션 생성 */
    private void createOptions(Product product) {
        Category category = getDefaultCategory();

        for (Gender gender : Gender.values()) {
            for (Size size : Size.values()) {

                ProductManagement pm = ProductManagement.of(
                        product,
                        null,
                        category,
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

