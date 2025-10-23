package JOO.jooshop.global.dummydata.util;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.product.model.ProductRequestDto;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


/**
 * ProductDataUtil 클래스는 더미 상품 데이터를 생성하고 DB에 저장하는 역할을 합니다.
 * ProductRepositoryV1을 이용해 생성한 Product 엔티티를 저장합니다.
 * 생성된 상품 데이터는 이미지 경로 정보를 기반으로 합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductDataUtil {

    private final ProductRepositoryV1 productRepository;

    /**
     * 이미지 경로를 기반으로 Product 엔티티를 생성하고 DB에 저장합니다.
     * @param imagePaths 이미지 경로 리스트
     * @return 저장된 Product 엔티티 리스트
     */
    public List<Product> generateProductDataWithImages(List<String> imagePaths) {
        return imagePaths.stream()
                .map(this::createProductFromImagePath)
                .map(this::saveProduct)
                .collect(Collectors.toList());
    }

    /**
     * 이미지 경로를 기반으로 Product 엔티티를 생성합니다. (DTO 기반)
     * @param imagePath 이미지 파일 경로 (카테고리 정보 포함)
     * @return 생성된 Product 엔티티
     */
    private Product createProductFromImagePath(String imagePath) {
        String[] splitPath = imagePath.split("_");
        if (splitPath.length < 2) {
            throw new IllegalArgumentException("잘못된 이미지 경로 형식: " + imagePath);
        }
        String subCategory = splitPath[1];
        Random rand = new Random();

        // ProductType 결정
        ProductType productType = getProductType(subCategory);

        // 기본 정보
        String productName = productType + " - " + subCategory;
        String productInfo = "official " + productType.toString().toLowerCase() + " product for " + subCategory;
        String manufacturer = "Manufacturer for " + subCategory;

        // 랜덤 가격 및 할인율 설정
        int[] prices = {200, 300, 400, 500};
        int[] discountRates = {10, 20, 30, 40, 50};

        BigDecimal price = BigDecimal.valueOf(prices[rand.nextInt(prices.length)]);
        boolean isDiscount = rand.nextBoolean();
        Integer discountRate = isDiscount ? discountRates[rand.nextInt(discountRates.length)] : null;
        boolean isRecommend = rand.nextBoolean();
        // int stock = rand.nextInt(50) + 1; // 1~50 worh
        Long wishListCount = 0L;

        // Builder 로 Product 생성
        Product product = Product.builder()
                .productType(productType)
                .productName(productName)
                .productInfo(productInfo)
                .manufacturer(manufacturer)
                .price(price)
                .isDiscount(isDiscount)
                .discountRate(discountRate)
                .isRecommend(isRecommend)
                .wishListCount(wishListCount)
                .build();

        return product;
    }

    /**
     * Product 엔티티를 DB에 저장하고, 제약조건 위반 시 로그를 남깁니다.
     */
    private Product saveProduct(Product product) {
        log.info("상품 저장: {}", product.getProductName());
        try {
            return productRepository.save(product);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof ConstraintViolationException cve) {
                log.error("상품 제약조건 위반: {}", product.getProductName(), cve.getSQLException());
            }
            throw ex;
        }
    }

    /**
     * 서브카테고리 문자열을 기반으로 ProductType 결정
     * 축구 용품/유니폼 관련
     */
    private ProductType getProductType(String subCategory) {
        if (subCategory == null || subCategory.isBlank()) {
            return ProductType.ACCESSORY; // 기본값
        }

        // 하이픈(-)을 언더스코어(_)로 바꾸고 대문자로 변환
        String normalized = subCategory.trim().toUpperCase().replace("-", "_");

        return switch (normalized) {
            case "HOME_JERSEY" -> ProductType.HOME_JERSEY;
            case "AWAY_JERSEY" -> ProductType.AWAY_JERSEY;
            case "THIRD_JERSEY" -> ProductType.THIRD_JERSEY;
            case "LONG_SLEEVE" -> ProductType.LONG_SLEEVE;
            case "TRAINING_WEAR", "TRAINING_JERSEY", "TRACK_SUIT", "WARM_UP_JERSEY" -> ProductType.TRAINING_WEAR;
            default -> ProductType.ACCESSORY;
        };
    }



}
