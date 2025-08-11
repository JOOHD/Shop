package JOO.jooshop.cart.entity;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.productManagement.entity.ProductManagement;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@Table(name = "cart")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long cartId; // PK

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member; // 회원

    @ManyToOne
    @JoinColumn(name = "Product_Mgt_id", nullable = false)
    private ProductManagement productManagement; // 상품

    @Column(name = "quantity", nullable = false)
    @Min(value = 0L)
    private int quantity; // 수량

    @Column(name = "price", nullable = false)
    private BigDecimal price; // 가격

    public Cart(Member member, ProductManagement productManagement, int quantity, BigDecimal price) {
        this.member = member;
        this.productManagement = productManagement;
        this.quantity = quantity;
        this.price = price;
    }


    // 수량 입력
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // 가격 입력
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

}
