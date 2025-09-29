package JOO.jooshop.admin.products.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AdminProductViewController {

    private final AdminProductService productService;

    @GetMapping
    public String listPage() {
        return "admin/product/product-list"; // Thyleaf 뷰
    }

    @GetMapping("/new")
    public String createPage() {
        return "admin/product/product-form"; // 등록 페이지
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        model.addAttribute("productId", id);
        return "admin/product/product-edit";
    }
}
