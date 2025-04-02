package JOO.jooshop.global.dummydata.util;


import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.ProductColor;
import JOO.jooshop.product.repository.ProductColorRepositoryV1;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.entity.enums.Size;
import JOO.jooshop.productManagement.repository.ProductManagementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductManagementDataUtil {

    private final ProductManagementRepository productManagementRepository;
    private final ProductColorRepositoryV1 productColorRepository;
    private final Random random = new Random();

    /**
     * Generates ProductManagement entities for the given products, image paths, and colors.
     *
     * @param products    the list of Product objects
     * @param imagePaths  the list of image paths
     * @param colors      the list of colors
     */
    @Transactional
    public void generateProductManagementData(List<Product> products, List<String> imagePaths, List<ProductColor> colors, List<Category> categories) {
        // Get three random colors
        List<ProductColor> randomColors = getRandomThreeColors(colors);

        for (int i = 0; i < imagePaths.size(); i++) {
            // 상품 n 번 + 썸네일 n 번, 색상 3가지에 대해서 상품인벤토리 생성
            String imagePath = imagePaths.get(i);
            Product product = products.get(i);
            // 랜덤한 3가지 색상에 대해 ProductManagement 생성.
            for (ProductColor color : randomColors) {
                ProductManagement productManagement = createProductManagementV2(product, imagePath, color, categories);
                productManagementRepository.save(Objects.requireNonNull(productManagement));
            }
        }
    }

    private List<ProductColor> getRandomThreeColors(List<ProductColor> colors) {
        Collections.shuffle(colors);
        return colors.stream().limit(3).collect(Collectors.toList());
    }

    private ProductManagement createProductManagementV2(Product product, String imagePath, ProductColor color, List<Category> categories) {
        long additionalStock = 0;
        boolean isRestockAvailable = true;
        boolean isRestocked = false;
        boolean isSoldOut = false;
        long initialStock = random.nextInt(1000);
        long productStock = initialStock;

        String[] splitPath = imagePath.split("_");
        String category = splitPath[0];
        String subCategory = splitPath[1];

        // Subcategory matching using the categories list
        Category matchedCategory = null;
        for (Category cat : categories) {
            if (cat.getName().equals(subCategory)) {
                matchedCategory = cat;
                break;
            }
        }

        // If no matching category found, return null or throw an exception
        if(matchedCategory == null) {
            return null; // or throw exception
        }

        // Random size selection
        Size size = Size.values()[random.nextInt(Size.values().length)];

        // Preset color selection
//        ProductColor productColor = new ProductColor(color);

        return new ProductManagement(initialStock, additionalStock, matchedCategory, product, productStock, size, color, isRestockAvailable, isRestocked, isSoldOut);
    }

    public ProductColor getRandomColor() {
        List<ProductColor> colors = productColorRepository.findAll();
        int randomIndex = new Random().nextInt(colors.size());
        return colors.get(randomIndex);
    }

//    private List<ProductColor> getRandomThreeProductColors() {
//        List<ProductColor> allColors = productColorRepository.findAll();
//        Collections.shuffle(allColors);
//        return allColors
//                .stream()
//                .limit(3)
//                .collect(Collectors.toList());
//    }

    /**
     * Creates a new instance of ProductManagement with random values for fields.
     *
     * @param index the index for creating the ProductManagement instance
     * @return a new instance of ProductManagement
     */
    private ProductManagement createProductManagementV1(int index) {
        long additionalStock = random.nextInt(500);
        boolean isRestockAvailable = random.nextBoolean();
        boolean isRestocked = random.nextBoolean();
        boolean isSoldOut = random.nextBoolean();
        long initialStock = random.nextInt(1000);
        long productStock = random.nextInt(1000);

        // XSMALL("X-Small"), SMALL("Small"), MEDIUM("Medium"), LARGE("Large"), XLARGE("X-Large"), FREE("Free")
        // 랜덤하게 입력.
        Size size = Size.values()[random.nextInt(Size.values().length)];

        long category_id = random.nextInt(34) + 1; // generate random category id between 1 and 34
        long product_id = random.nextInt(500) + 1; // generate random product id between 1 and 500
        // category_id 만을 가지고 있는 Category
        Category categoryById = Category.createCategoryById(category_id);
        // product_id 만을 가지고 있는 Product
        Product productById = Product.createProductById(product_id);
        // product color 를 1번부터 14번까지 색상중 랜덤하게 들고옵니다.
        ProductColor color = getRandomColor();

        return new ProductManagement(initialStock, additionalStock, categoryById, productById, productStock, size, color, isRestockAvailable, isRestocked, isSoldOut);
    }


}
