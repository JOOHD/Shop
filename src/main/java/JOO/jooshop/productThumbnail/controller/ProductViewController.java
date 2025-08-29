package JOO.jooshop.productThumbnail.controller;

import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.model.ProductDetailDto;
import JOO.jooshop.product.service.ProductServiceV1;
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

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductViewController { // view 용 컨트롤러

    private final JWTUtil jwtUtil;
    private final ProductServiceV1 productService;
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

    @GetMapping("/{productId}")
    public String productDetail(@PathVariable("productId") Long productId,
                                @CookieValue(name = "accessAuthorization", required = false) String accessTokenWithPrefix,
                                Model model) {

        ProductDetailDto productDetail = productService.productDetail(productId);

        model.addAttribute("product", productDetail);
        model.addAttribute("sizes", productDetail.getSizes()); // DTO에서 제공하는 사이즈 리스트

        String memberId = null;
        try {
            if (accessTokenWithPrefix != null && accessTokenWithPrefix.startsWith("Bearer+")) {
                String accessToken = accessTokenWithPrefix.replace("Bearer+", "");
                if (jwtUtil.validateToken(accessToken)) {
                    memberId = jwtUtil.getMemberId(accessToken);
                }
            }
        } catch (Exception e) {
            log.warn("JWT 파싱 실패", e);
        }

        model.addAttribute("memberId", memberId);

        return "products/productDetail";
    }

}
