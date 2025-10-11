package JOO.jooshop.admin.products.controller;

import JOO.jooshop.admin.products.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class AdminProductViewController {

    private final AdminProductService productService;

    /* 상품 Thymeleaf 뷰 */
    @GetMapping
    public String listPage() {
        return "admin/products/productList";
    }

    /* 상품 등록 페이지 */
    @GetMapping("/new")
    public String createPage() {
        return "admin/products/productForm";
    }

    /* 상품 수정 페이지 */
    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        model.addAttribute("productId", id);
        return "admin/product/productEdit";
    }
}
