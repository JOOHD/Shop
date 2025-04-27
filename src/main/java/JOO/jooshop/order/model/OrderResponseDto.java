package JOO.jooshop.order.model;

import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.order.entity.enums.PayMethod;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.productManagement.entity.ProductManagement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDto {
    Long orderId;
    Long memberId;
    List<Long> productMgtIds;
    List<Long> productIds;
    String ordererName;
    String ordererEmail;
    String ordererPhone;
    String productName;
    PayMethod payMethod;
    String merchantUid;
    BigDecimal totalPrice;
    String postCode;
    String address;
    String detailAddress;
    LocalDateTime orderDay;
    Boolean paymentStatus;

}
