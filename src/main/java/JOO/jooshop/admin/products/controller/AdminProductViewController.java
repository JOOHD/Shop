package JOO.jooshop.admin.products.controller;

import JOO.jooshop.admin.products.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductViewController { // 페이지 라우팅

    private final AdminProductService productService;

    /* 상품 목록 */
    @GetMapping("/list")
    public String listPage() {
        return "admin/products/productList";
    }

    /* 상품 등록 */
    @GetMapping("/register")
    public String registerPage() {
        return "admin/products/productRegister";
    }

    /* 상품 수정 */
    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Long id, Model model) {
        model.addAttribute("productId", id);
        return "admin/product/productEdit";
    }
}
