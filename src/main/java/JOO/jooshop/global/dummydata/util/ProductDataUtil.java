package JOO.jooshop.global.dummydata.util;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * The ProductDataUtil class is responsible for generating and saving product data. It uses the ProductRepositoryV1
 * interface to save each product to the database. The generated product data is based on the given count parameter.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductDataUtil {

    private final ProductRepositoryV1 productRepository;

    public static final String PRODUCT_NAME_TEMPLATE = "Product %d";
    public static final String PRODUCT_INFO_TEMPLATE = "This is a %s product";
    public static final String MANUFACTURER_NAME_TEMPLATE = "Manufacturer %d";

//    public List<Product> generateProductData(int count) {
//        return IntStream.rangeClosed(1, count)
//                .mapToObj(this::createProduct)
//                .map(this::saveProduct)
//                .collect(Collectors.toList());
//    }

//    private Product createProduct(int index) {
//        ProductType productType = getProductType(index);
//        String productName = buildProductName(index);
//        String productInfo = buildProductInfo(productType);
//        String manufacturer = buildManufacturerName(index);
//        int price = computePrice(index);
//        boolean isDiscount = isDiscount(index);
//        boolean isRecommend = isRecommend(index);
//        return Product.createDummyProduct(productName, productType, price, productInfo, manufacturer, isDiscount, isRecommend);
//    }

    public List<Product> generateProductDataWithImages(List<String> imagePaths) {
        return imagePaths.stream()
                .map(this::createProductFromImagePath)
                .map(this::saveProduct)
                .collect(Collectors.toList());
    }


    private Product createProductFromImagePath(String imagePath) {
        String[] splitPath = imagePath.split("_");
        String category = splitPath[0];
        String subCategory = splitPath[1];
        Random rand = new Random();

        ProductType productType = getProductType(subCategory);
        String productName = "Product for " + subCategory;
        String productInfo = "This is a " + productType.toString().toLowerCase() + " product for " + subCategory;
        String manufacturer = "Manufacturer for " + subCategory;
        // 가격을 100, 200, 300, 400, 500 중에서 랜덤하게 select.
        int[] prices = {200, 300, 400, 500};
        int[] discountRates = {10, 20, 30, 40, 50};

        BigDecimal price = BigDecimal.valueOf(prices[rand.nextInt(prices.length)]);

        boolean isDiscount = rand.nextBoolean();
        // isDiscount 여부에 따라, discountRate 를 정해주도록 리펙토링 [24.06.03]
        Integer discountRate;
        if (isDiscount) {
            discountRate = discountRates[rand.nextInt(discountRates.length)];
        } else {
            discountRate = null;
        }

        boolean isRecommend = rand.nextBoolean();

        // You might want to set imagePath to your product here if you have such field in your Product entity
        return new Product(productName, productType, price, productInfo, manufacturer, isDiscount, discountRate, isRecommend);
    }

    // 단순히 DB 에 저장하는 로직
    private Product saveProduct(Product product) {
        log.info("Product details: " + product.toString());

        try {
            return productRepository.save(product);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof ConstraintViolationException) {
                ConstraintViolationException constraintException = (ConstraintViolationException) ex.getCause();
                log.error("Constraint violation for Product: " + product.toString(), constraintException.getSQLException());
            }
            throw ex;
        }
    }

    private ProductType getProductType(String subCategory) {
        return switch (subCategory) {
            case "blouse", "skirt", "dress", "two-piece", "short-padding", "boots" -> ProductType.WOMAN;
            case "hoodie", "knit-sweater", "long-shirts", "long-sleeve", "short-shirts", "short-sleeve", "sweatshirt",
                 "long", "shorts", "set-up", "cardigan", "coat", "jacket", "lightweight-padding", "long-padding",
                 "vest", "sandal", "sneakers", "bag", "cap", "socks" -> ProductType.UNISEX;
            default -> ProductType.MAN;
        };
    }

    private String buildProductName(int index) {
        return String.format(PRODUCT_NAME_TEMPLATE, index);
    }

    private String buildProductInfo(ProductType productType) {
        return String.format(PRODUCT_INFO_TEMPLATE, productType.toString().toLowerCase());
    }

    private String buildManufacturerName(int index) {
        return String.format(MANUFACTURER_NAME_TEMPLATE, index);
    }

}
