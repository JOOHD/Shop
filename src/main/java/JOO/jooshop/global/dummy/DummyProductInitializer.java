package JOO.jooshop.global.dummy;

import JOO.jooshop.global.file.FileStorageService;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.Gender;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.entity.enums.Size;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productManagement.repository.ProductManagementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@Profile("local") // local 환경에서만 실행
@Component
@RequiredArgsConstructor
public class DummyProductInitializer implements CommandLineRunner {

    /**
     * 상품 10개 생성
     * 옵션(ProductManagement) 자동 생성 (성별 + 사이즈 + 재고)
     * 이미지는 FileStorageService를 통해 외부 URL로 다운로드 후 저장
     * 예외 처리 포함 (저장 실패 시 로깅)
     */

    private final ProductRepositoryV1 productRepository;
    private final FileStorageService fileStorageService;
    private final ProductManagementRepository productManagementRepository;

    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // 더미 상품 이름 + 이미지 URL (외부)
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

        List<ProductType> productTypes = Arrays.asList(
                ProductType.HOME_JERSEY,
                ProductType.AWAY_JERSEY,
                ProductType.THIRD_JERSEY,
                ProductType.LONG_SLEEVE,
                ProductType.TRAINING_WEAR,
                ProductType.ACCESSORY,
                ProductType.ACCESSORY,
                ProductType.ACCESSORY,
                ProductType.ACCESSORY,
                ProductType.ACCESSORY
        );

        Gender[] genders = Gender.values(); // MAN, WOMAN, UNISEX
        List<Size> sizes = Arrays.asList(Size.XS, Size.S, Size.M, Size.L, Size.XL, Size.XXL, Size.FREE);

        for (int i = 0; i < productNames.size(); i++) {
            try {
                String productName = productNames.get(i);
                String imageUrl = imageUrls.get(i);

                ProductType type = ProductType.values()[random.nextInt(ProductType.values().length)];

                Product product = Product.createDummy(
                        productName,
                        type,
                        BigDecimal.valueOf(100_000 + random.nextInt(200_000)),
                        productName + " 상세 정보",
                        "MANUTD Official",
                        random.nextBoolean(),
                        random.nextInt(50),
                        random.nextBoolean()
                );

                // 썸네일 이미지 다운로드
                try {
                    String thumbnailPath = fileStorageService.saveFileFromUrl(new URL(imageUrl), "thumbnails");
                    log.info("Thumbnail saved for {} -> {}", productName, thumbnailPath);
                } catch (IOException e) {
                    log.warn("Thumbnail download failed for {}: {}", productName, e.getMessage());
                }

                productRepository.save(product);
                // 옵션 생성
                for (Gender gender : Gender.values()) {
                    for (Size size : Size.values()) {
                        Long stock = 10L + random.nextInt(20); // 10~30 랜덤
                        ProductManagement pm = ProductManagement.of(
                                product,
                                null, // 컬러 없이
                                null, // 카테고리 없으면 null
                                size,
                                stock,
                                true,
                                false,
                                false
                        );
                        pm.setGender(gender);
                        productManagementRepository.save(pm);
                    }
                }

            } catch (Exception ex) {
                log.error("Dummy product init failed at index {}: {}", i, ex.getMessage(), ex);
            }
        }
    }
}
