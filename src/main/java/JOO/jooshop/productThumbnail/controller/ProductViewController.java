package JOO.jooshop.productThumbnail.controller;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productThumbnail.entity.ProductThumbnail;
import JOO.jooshop.productThumbnail.repository.ProductThumbnailRepositoryV1;
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
    private final ProductThumbnailRepositoryV1 thumbnailRepository;

    @GetMapping("/{productId}")
    public String productPage(@PathVariable Long productId, Model model) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("상품 없음"));

        List<String> thumbnails = thumbnailRepository.findByProduct_ProductId(productId)
                .stream()
                .map(ProductThumbnail::getImagePath)
                .toList();

        model.addAttribute("product", product);
        model.addAttribute("thumbnailPaths", thumbnails);
        return "products/product"; // product.html
    }
}
