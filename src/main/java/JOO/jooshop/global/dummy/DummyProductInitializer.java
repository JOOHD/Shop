package JOO.jooshop.global.dummy;

import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.categorys.repository.CategoryRepository;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.Gender;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.entity.enums.Size;
import JOO.jooshop.productManagement.repository.ProductManagementRepository;
import JOO.jooshop.thumbnail.entity.ProductThumbnail;
import JOO.jooshop.thumbnail.repository.ProductThumbnailRepositoryV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Slf4j
@Profile("local")
@Component
@RequiredArgsConstructor
public class DummyProductInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepositoryV1 productRepository;
    private final ProductThumbnailRepositoryV1 productThumbnailRepository;
    private final ProductManagementRepository productManagementRepository;

    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) {
        log.info("[DummyProductInitializer] START");

        resetDummyData();

        Category dummyCategory = getOrCreateDefaultCategory();
        createDummyProducts(dummyCategory);

        log.info("[DummyProductInitializer] END");
    }

    private Category getOrCreateDefaultCategory() {
        return categoryRepository.findByName("DUMMY")
                .orElseGet(() -> categoryRepository.save(Category.ofName("DUMMY")));
    }

    /**
     * ✅ 더미 데이터만 삭제
     * - Product.dummy=true 대상만 골라서
     * - FK 테이블 → Product 순으로 삭제
     *
     * 주의:
     * - ProductThumbnailRepositoryV1은 deleteByProductId(List<Long>)가 "이미 존재"
     * - ProductManagementRepository는 현재 시그니처가 불명확(네가 에러로 언급),
     *   그래서 "가장 안전한" 단건 루프 방식으로 처리
     */
    protected void resetDummyData() {
        log.info("[DummyProductInitializer] delete dummy data only");

        List<Long> dummyIds = productRepository.findDummyIds();
        if (dummyIds == null || dummyIds.isEmpty()) {
            log.info("[DummyProductInitializer] no dummy data to delete");
            return;
        }

        // 1) 옵션 삭제 (리포지토리 시그니처가 List를 못 받는 상태라면 단건 루프)
        // - 만약 deleteByProductId(@Param("ids") List<Long> ids) 형태가 이미 있다면
        //   아래 루프를 "한 번 호출"로 바꿀 수 있음.
        for (Long id : dummyIds) {
            try {
                productManagementRepository.deleteByProductId(id);
            } catch (Exception e) {
                log.warn("[DummyProductInitializer] delete options failed. productId={}", id, e);
            }
        }

        // 2) 썸네일 삭제 (너가 보여준 메서드는 List<Long> 받는 JPQL delete)
        try {
            productThumbnailRepository.deleteByProductIds(dummyIds);
        } catch (Exception e) {
            log.warn("[DummyProductInitializer] delete thumbnails failed. ids={}", dummyIds.size(), e);
        }

        // 3) Product 삭제
        productRepository.deleteAllById(dummyIds);

        log.info("[DummyProductInitializer] deleted dummy products: {}", dummyIds.size());
    }

    private void createDummyProducts(Category dummyCategory) {
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

        int count = Math.min(productNames.size(), imageUrls.size());
        if (productNames.size() != imageUrls.size()) {
            log.warn("[Dummy] productNames({}) != imageUrls({}) -> using {}",
                    productNames.size(), imageUrls.size(), count);
        }

        for (int i = 0; i < count; i++) {
            String name = productNames.get(i);
            String url = imageUrls.get(i);

            try {
                // ✅ Product.createDummy 시그니처는 8개 인자 (네가 제공한 코드 기준)
                Product product = createProduct(name);

                // ✅ product 저장 먼저 (FK 안정화)
                Product saved = productRepository.save(product);

                // ✅ 썸네일 저장: ProductThumbnail 기본 생성자 protected → (product, url) 생성자 사용
                addThumbnail(saved, url);

                // ✅ 옵션 저장: ProductManagement.of(...)는 "9개 + 순서 고정" (아래에서 맞춤)
                createOptions(saved, dummyCategory);

                log.info("[Dummy] created product: {}", saved.getProductName());
            } catch (Exception e) {
                log.error("[Dummy] failed product: {}", name, e);
            }
        }
    }

    private Product createProduct(String productName) {
        return Product.createDummy(
                productName,
                ProductType.values()[random.nextInt(ProductType.values().length)],
                BigDecimal.valueOf(100_000 + random.nextInt(200_000)),
                productName + " 상세 정보",
                "MANUTD Official",
                true,
                random.nextInt(50),
                true
        );
    }

    private void addThumbnail(Product product, String imageUrl) {
        String normalized = normalizeUrl(imageUrl);

        if (normalized == null) {
            log.warn("[Dummy] skip invalid thumbnail url. product={}", product.getProductName());
            return;
        }

        ProductThumbnail thumbnail = ProductThumbnail.create(product, normalized);
        productThumbnailRepository.save(thumbnail);
    }

    private String normalizeUrl(String url) {
        if (url == null) return null;

        String trimmed = url.trim();
        if (trimmed.isBlank()) return null;

        if (!(trimmed.startsWith("http://") || trimmed.startsWith("https://"))) return null;

        return trimmed;
    }

    private void createOptions(Product product, Category dummyCategory) {
        for (Gender gender : Gender.values()) {
            for (Size size : Size.values()) {

                // ✅ 너가 준 ProductManagement.of 시그니처 그대로 맞춤
                // (product, color, category, gender, size, initialStock, restockAvailable, restocked, soldOut)
                ProductManagement pm = ProductManagement.of(
                        product,
                        null,          // ⚠️ 여기 null이면 validateRequired에서 터짐!
                        dummyCategory,
                        gender,
                        size,
                        20L,
                        true,
                        false,
                        false
                );

                productManagementRepository.save(pm);
            }
        }
    }
}
