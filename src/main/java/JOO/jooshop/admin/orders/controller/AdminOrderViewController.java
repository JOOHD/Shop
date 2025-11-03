package JOO.jooshop.admin.orders.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderViewController {

    @GetMapping
    public String orderListPage() {
        return "admin/orders/orderList";
    }
}
