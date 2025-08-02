package JOO.jooshop.productThumbnail.controller;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productThumbnail.entity.ProductThumbnail;
import JOO.jooshop.productThumbnail.service.ProductThumbnailServiceV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductViewController { // view 용 컨트롤러

    private final ProductRepositoryV1 productRepository;
    private final ProductThumbnailServiceV1 productThumbnailService;

    @GetMapping
    public String productList(Model model) { // 상품 전체 조회
        List<Product> products = productThumbnailService.getAllProductsWithThumbnails();

        // 로그용 확인
        for (Product product : products) {
            log.info("Product: {}", product.getProductName());
            for (ProductThumbnail thumb : product.getProductThumbnails()) {
                log.info("Thumbnail: {}", thumb.getImagePath());
            }
        }
        model.addAttribute("products", products);
        return "products/productList";
    }

    @GetMapping("/{productId}") // 상품 상세
    public String productDetail(@PathVariable("productId") Long productId, Model model) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("상품 없음"));

        model.addAttribute("product", product);
        return "products/productDetail"; // productList.html
    }
}
