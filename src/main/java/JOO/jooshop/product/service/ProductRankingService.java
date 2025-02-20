package JOO.jooshop.product.service;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.model.ProductRankResponseDto;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
@Slf4j
public class ProductRankingService {

    /*
        1. 상품 조회수 증가 -> 사용자가 상품을 볼 떄마다 조회수를 올림.
        2. 상품 랭킹 조회 -> 조회수가 많은 상품을 상위순으로 가져옴.
        3. 상품 리스트 조회(랭킹순으로) -> 랭킹에 따라 상품 목록을 가져오고, DTO 로 변환해 반환.

        Redis : 실시간 랭킹 같은 빠른 데이터 처리를 위해 사용. ZSet(정렬된 집합) 활용
        ModelMapper : Entity -> DTO 변환을 편하게 하기 위해 사용
     */

    public final ProductRepositoryV1 productRepository;
    public final ProductColorRepository productColorRepository;
    public final ModelMapper modelMapper;
    private final RedisTemplate<String, String> redisTemplate;

    // 상품 조회수 증가 메서드
    public void increaseProductViews(Long productId) {
        String key = "product_views"; // Redis 매핑
        // ProductServiceV1 class 의 productDetail 메서드에 호출
        redisTemplate.opsForZSet().incrementScore(key, String.valueOf(productId), 1);
    }

    // 랭킹을 위한 상품 조회수 가져오는 메서드
    public Set<String> getTopProductIds(int limit) {
        String key = "product_views";
        // Redis 에 있는 key 값을 ZSet 에서 '조회수가 높은 순서'로 상품 ID를 가져온다.
        return redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);
    }

    // 랭킹순으로 상품 리스트를 조회하는 메서드
    public List<ProductRankResponseDto> getProductListByRanking(int limit) {

        Set<String> productIds = getTopProductIds(limit);
        // Redis에서 가져온 ID는 문자열이라서 **숫자(Long)**으로 변환.
        List<Long> productIdList = productIds.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());

        // 상품 ID로 DB에서 상품 조회, 해당 상품이 없으면 null 반환.
        List<Product> products = productIdList.stream()
                .map(productId -> productRepository.findByProductId(productId).orElse(null))
                .toList();

        return products.stream()
                .map(product -> { // Product -> ProductRankResponseDto 변환
                    ProductRankResponseDto ProductRankResponseDto = modelMapper.map(product, ProductRankResponseDto.class);
                    // ProductThumbnail의 imagePath를 매핑
                    ProductRankResponseDto.setProductThumbnails(
                            product.getProductThumbnails().get(0).getImagePath());
                    return ProductRankResponseDto;
                })
                .toList();
    }
}














