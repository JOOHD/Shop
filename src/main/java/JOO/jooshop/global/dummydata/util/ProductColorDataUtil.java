package JOO.jooshop.global.dummydata.util;


import JOO.jooshop.product.entity.ProductColor;
import JOO.jooshop.product.repository.ProductColorRepositoryV1;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductColorDataUtil {

    private final ProductColorRepositoryV1 productColorRepository;

    @Transactional
    public List<ProductColor>  generateAndSaveProductColorData() {
        // Red, Orange, Yellow, Green, Blue, Navy, Purple, Black, White, Gray, Pink, Ivory, Beige, Rainbow
        return Arrays.asList(
                createAndSaveProductColor("Red"),
                createAndSaveProductColor("Orange"),
                createAndSaveProductColor("Yellow"),
                createAndSaveProductColor("Green"),
                createAndSaveProductColor("Blue"),
                createAndSaveProductColor("Navy"),
                createAndSaveProductColor("Purple"),
                createAndSaveProductColor("Black"),
                createAndSaveProductColor("White"),
                createAndSaveProductColor("Gray"),
                createAndSaveProductColor("Pink"),
                createAndSaveProductColor("Ivory"),
                createAndSaveProductColor("Beige"),
                createAndSaveProductColor("Rainbow")
        );
    }

    private ProductColor createAndSaveProductColor(String color) {
        Optional<ProductColor> existingProductColor = productColorRepository.findByColor(color);
        if (existingProductColor.isEmpty()) {
            ProductColor productColor = new ProductColor(color);
            return productColorRepository.save(productColor);
        } else {
            return existingProductColor.get();
        }
    }
}
