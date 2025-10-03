package JOO.jooshop.product.service;

import JOO.jooshop.contentImgs.service.ContentImgService;
import JOO.jooshop.global.authorization.RequiresRole;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.ProductColor;
import JOO.jooshop.product.model.*;
import JOO.jooshop.product.model.ProductApiDto;
import JOO.jooshop.product.repository.ProductColorRepositoryV1;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productThumbnail.service.ProductThumbnailServiceV1;
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

import static JOO.jooshop.global.Exception.ResponseMessageConstants.PRODUCT_NOT_FOUND;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
@Slf4j
public class ProductServiceV1 {

    /**
     * ProductRequestDto → 클라이언트가 보내는 요청용 DTO
     * ProductApiDto → API 응답용 DTO (상품 상세, 옵션, 썸네일 포함)
     * ProductListDto → 상품 목록 조회용 DTO (대표 썸네일 + 최소 정보)
     */

    public final ProductRepositoryV1 productRepository;
    public final ProductColorRepositoryV1 productColorRepository;
    public final ModelMapper modelMapper;
    private final ProductThumbnailServiceV1 productThumbnailService;
    private final ContentImgService contentImgService;
    private final ProductRankingService productRankingService;

    /**
     * 상품 등록
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public Long createProduct(ProductRequestDto requestDto,
                              @Nullable List<MultipartFile> thumbnailImgs,
                              @Nullable List<MultipartFile> contentImgs) {

        if (requestDto.getPrice() == null || requestDto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("가격은 0 이상이어야 합니다.");
        }

        Product product = new Product(requestDto);
        productRepository.save(product);

        // 추가 - 썸네일 저장 메서드 실행
        if (thumbnailImgs != null && !thumbnailImgs.isEmpty() &&
            !Objects.equals(thumbnailImgs.get(0).getOriginalFilename(), "")) {
            productThumbnailService.uploadThumbnail(product, thumbnailImgs);
        }

        if (contentImgs != null && !contentImgs.isEmpty() &&
            !Objects.equals(contentImgs.get(0).getOriginalFilename(), "")) {
            contentImgService.uploadContentImage(product, contentImgs);
        }

        return product.getProductId();
    }

    /**
     * 상품 목록 조회(전체)
     */
    @Transactional(readOnly = true)
    public List<ProductListDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductListDto::new) // new ProductApiDto(product) 호출됨
                .collect(Collectors.toList());
    }

    /**
     *  상품 상세 조회
     */
    @Transactional(readOnly = true)
    public ProductApiDto productDetail(Long productId) {
        // 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND));

        // 상품 조회 시 조회수 증가
        log.info("View Increment");
        productRankingService.increaseProductViews(productId);

        // ProductManagement 옵션 전체 가져오기
        List<ProductManagement> productMgtList = product.getProductManagements();
        if (productMgtList.isEmpty()) {
            throw new NoSuchElementException("상품 옵션을 찾을 수 없습니다.");
        }

        // 대표 이미지 (없으면 빈 문자열)
        String thumbnailUrl = product.getProductThumbnails().stream()
                .findFirst()
                .map(t -> t.getImagePath().startsWith("/") ? t.getImagePath().substring(1) : t.getImagePath())
                .orElse("");

        // DTO 생성
        ProductApiDto dto = new ProductApiDto(product, productMgtList, thumbnailUrl);

        // 첫 번째 옵션의 inventoryId 세팅
        dto.withInventoryId(productMgtList.get(0).getInventoryId());

        return dto;
    }


    /**
     * 상품 수정
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public ProductApiDto updateProduct(Long productId, ProductRequestDto updatedDto) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND));

        // RequestDto -> ProductDto 변환
        ProductDto dto = new ProductDto(updatedDto)
        existingProduct.updateFromDto(updatedDto);
        productRepository.save(existingProduct);

        return new ProductApiDto(existingProduct);
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
