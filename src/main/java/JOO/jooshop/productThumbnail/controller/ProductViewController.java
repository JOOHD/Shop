package JOO.jooshop.productThumbnail.controller;

import JOO.jooshop.global.Exception.ResponseMessageConstants;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.model.ProductDetailDto;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productThumbnail.entity.ProductThumbnail;
import JOO.jooshop.productThumbnail.service.ProductThumbnailServiceV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
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

    private final JWTUtil jwtUtil;
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
    public String productDetail(@PathVariable("productId") Long productId,
                                @CookieValue(name = "accessAuthorization", required = false) String accessTokenWithPrefix,
                                Model model) {
        // 사이즈 조회
        List<ProductDetailDto> details = productRepository.findProductDetailById(productId);
        if (details.isEmpty()) {
            throw new NoSuchElementException(ResponseMessageConstants.PRODUCT_NOT_FOUND);
        }

        model.addAttribute("sizes", details);

        // JWT 에서 memberId 추출
        if (accessTokenWithPrefix != null && accessTokenWithPrefix.startsWith("Bearer+")) {
            String accessToken = accessTokenWithPrefix.replace("Bearer+", "");

            if (jwtUtil.validateToken(accessToken)) {
                String memberId = jwtUtil.getMemberId(accessToken);
                model.addAttribute("memberId", memberId); // view 에서 활용
            } else {
                model.addAttribute("memberId", null);
            }
        } else {
            model.addAttribute("memberId", null); // 토큰이 없거나 형식이 잘못된 경우
        }

        return "products/productDetail"; // productList.html
    }
}
