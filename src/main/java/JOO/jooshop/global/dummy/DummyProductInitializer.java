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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@Profile("local") // local 환경에서만 실행
@Component
@RequiredArgsConstructor
public class DummyProductInitializer implements CommandLineRunner {

    /**
     * 클래스 목적
     * 데이터 초기화, 로컬 환경에 빠른 테스트용 상품 데이터 -> DB 저장
     * 
     * 1. CommandLineRunner + @Profile("local") → 로컬 실행 시 자동 실행
     * 2. 10개 더미 상품 생성 (Product.createDummy(...))
     * 3. ProductRepository에 저장 (productRepository.save(product))
     * 4. 옵션(ProductManagement) 자동 생성 → Gender x Size 조합
     * 5. 외부 URL 그대로 DTO에서 사용 가능 → 썸네일 URL을 DB에 저장하지 않고 로그로 확인
     * 6. FileStorageService 제거 → 로컬 이미지 저장 없이 외부 URL 사용
     */

    private final ProductRepositoryV1 productRepository;
    private final ProductManagementRepository productManagementRepository;
    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // 상품명 + 외부 이미지 URL
        List<String> productNames = Arrays.asList(
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

        List<String> imageUrls = Arrays.asList(
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
                String productName = productNames.get(i);
                String thumbnailUrl = imageUrls.get(i);

                Product product = Product.createDummy(
                        productName,
                        ProductType.values()[random.nextInt(ProductType.values().length)],
                        BigDecimal.valueOf(100_000 + random.nextInt(200_000)),
                        productName + " 상세 정보",
                        "MANUTD Official",
                        true,             // isDiscount
                        random.nextInt(50),  // discountRate
                        true
                );
                // DB에 상품 저장
                productRepository.save(product);

                // 옵션 생성
                for (Gender gender : Gender.values()) {
                    for (Size size : Size.values()) {
                        ProductManagement pm = ProductManagement.of(
                                product,
                                null,
                                null,
                                size,
                                10L + random.nextInt(20),
                                true,
                                false,
                                false
                        );
                        pm.setGender(gender);
                        productManagementRepository.save(pm);

                        // dummyThumbnail 생성 (외부 URL만)
                        ProductThumbnail thumbnail = new ProductThumbnail(product, imageUrls.get(i));
                        product.addThumbnail(thumbnail);
                    }
                }

                // DTO에서 바로 외부 URL 사용 가능하므로 별도 저장 불필요
                log.info("Dummy product created: {} -> {}", productName, thumbnailUrl);

            } catch (Exception ex) {
                log.error("Dummy product init failed at index {}: {}", i, ex.getMessage(), ex);
            }
        }
    }
}
