package JOO.jooshop.productThumbnail.controller;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.product.service.ProductServiceV1;
import JOO.jooshop.productThumbnail.entity.ProductThumbnail;
import JOO.jooshop.productThumbnail.repository.ProductThumbnailRepositoryV1;
import JOO.jooshop.productThumbnail.service.ProductThumbnailServiceV1;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.NoSuchElementException;

@Controller
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductViewController {

    private final ProductRepositoryV1 productRepository;
    private final ProductThumbnailServiceV1 productThumbnailService;

    @GetMapping
    public String productList(Model model) {
        List<Product> products = productThumbnailService.getAllProductsWithThumbnails();
        model.addAttribute("products", products);
        return "products/productList";
    }

    @GetMapping("/{productId}") // 상품 상세
    public String productDetail(@PathVariable Long productId, Model model) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("상품 없음"));

        model.addAttribute("product", product);
        return "products/productDetail"; // productList.html
    }
}
