package JOO.jooshop.admin.products.repository;

import JOO.jooshop.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminProductRepository extends JpaRepository<Product, Long> {


}
