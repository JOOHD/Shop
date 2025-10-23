package JOO.jooshop.admin.products.controller;

import JOO.jooshop.admin.products.service.AdminProductService;
import JOO.jooshop.thumbnail.model.ProductThumbnailDto;
import JOO.jooshop.thumbnail.service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductViewController { // 페이지 라우팅

    private final AdminProductService productService;
    private final ThumbnailService thumbnailService;

    /* 상품 목록 */
    @GetMapping("/list")
    public String productList(Model model) {
        List<ProductThumbnailDto> thumbnails = thumbnailService.getAllThumbnails();

        model.addAttribute("thumbnails", thumbnails);
        return "admin/products/productList";
    }

    /* 상품 등록 */
    @GetMapping("/register")
    public String productRegister() {
        return "admin/products/productRegister";
    }

    /* 상품 수정 */
    @GetMapping("/edit/{id}")
    public String productEdit(@PathVariable Long id, Model model) {
        model.addAttribute("productId", id);
        return "admin/product/productEdit";
    }
}
