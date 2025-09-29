package JOO.jooshop.admin.products.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final AdminProductRepository productRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public void save(Product product) {
        productRepository.save(product);
    }
}
