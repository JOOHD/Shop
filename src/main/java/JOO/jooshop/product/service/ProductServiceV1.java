package JOO.jooshop.product.service;

import JOO.jooshop.contentImgs.entity.enums.UploadType;
import JOO.jooshop.contentImgs.service.ContentImgService;
import JOO.jooshop.global.authorization.RequiresRole;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.ProductColor;
import JOO.jooshop.product.model.*;
import JOO.jooshop.product.model.ProductDetailResponseDto;
import JOO.jooshop.product.repository.ProductColorRepositoryV1;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.thumbnail.service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import static JOO.jooshop.global.exception.ResponseMessageConstants.PRODUCT_NOT_FOUND;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
@Slf4j
public class ProductServiceV1 {

    /**
     * ProductRequestDto → 클라이언트가 보내는 요청용 DTO
     * ProductDetailResponseDto → API 응답용 DTO (상품 상세, 옵션, 썸네일 포함)
     * ProductListResponseDto → 상품 목록 조회용 DTO (대표 썸네일 + 최소 정보)
     */

    public final ProductRepositoryV1 productRepository;
    public final ProductColorRepositoryV1 productColorRepository;
    public final ModelMapper modelMapper;
    private final ThumbnailService thumbnailService;
    private final ContentImgService contentImgService;
    private final ProductRankingService productRankingService;

    /**
     * 상품 등록
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public Long createProduct(ProductRequestDto requestDto,
                              String thumbnailUrl,      // thumbnail 은 대표 1장
                              List<String> contentUrls, // contentImg 는 여러장
                              UploadType uploadType) {
        Product product = new Product(requestDto);
        productRepository.save(product);

        // 썸네일 처리
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            thumbnailService.uploadThumbnailImages(product, thumbnailUrl);
        }

        // 상세 이미지 처리
        if (contentUrls != null && !contentUrls.isEmpty()) {
            contentImgService.uploadContentImages(product, contentUrls, uploadType);
        }

        return product.getProductId();
    }


    /**
     * 상품 목록 조회(전체)
     */
    @Transactional(readOnly = true)
    public List<ProductListResponseDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductListResponseDto::new) // new ProductDetailResponseDto(product) 호출됨
                .collect(Collectors.toList());
    }

    /**
     *  상품 상세 조회
     *  1. N+1 문제 방지: fetch join으로 한 번에 모든 연관 엔티티 로딩
     *  2. DTO 변환 책임 분리: Repository는 엔티티만 가져오고, DTO 생성은 Service에서
     */
    @Transactional(readOnly = true)
    public ProductDetailResponseDto productDetail(Long productId) {
        // Repository 에서 fetch join 으로 연관 엔티티까지 한 번에 가져오기
        Product product = productRepository.findProductWithDetailsByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND));

        // 상품 조회 시. 조회수 증가
        productRankingService.increaseProductViews(productId);

        // ProductDetailResponseDto 생성
        ProductDetailResponseDto dto = new ProductDetailResponseDto(product);

        // ProductManagement 옵션 전체 가져오기
        List<ProductManagement> options = product.getProductManagements();
        if (!options.isEmpty()) {
            dto.withInventoryId(options.get(0).getInventoryId());
        }

        return dto;
    }


    /**
     * 상품 수정
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public ProductDetailResponseDto updateProduct(Long productId, ProductRequestDto updatedDto) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND));

        existingProduct.updateFromRequestDto(updatedDto);
        productRepository.save(existingProduct);

        return new ProductDetailResponseDto(existingProduct);
    }

    /**
     * 상품 삭제
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public void deleteProduct(Long productId) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND));

        productRepository.delete(existingProduct);
    }

    /**
     * 색상 등록
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public Long createColor(ProductColorDto request) {
        ProductColor color = modelMapper.map(request, ProductColor.class);
        productColorRepository.save(color);
        return color.getColorId();
    }

    /**
     * 색상 삭제
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public void deleteColor(Long colorId) {
        ProductColor color = productColorRepository.findById(colorId)
                .orElseThrow(() -> new NoSuchElementException("해당 색상을 찾을 수 없습니다. Id : " + colorId));
        productColorRepository.delete(color);
    }
}
