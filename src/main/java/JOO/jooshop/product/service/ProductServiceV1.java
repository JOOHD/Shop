package JOO.jooshop.product.service;

import JOO.jooshop.contentImgs.entity.enums.UploadType;
import JOO.jooshop.contentImgs.service.ContentImgService;
import JOO.jooshop.global.authorization.RequiresRole;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.ProductColor;
import JOO.jooshop.product.model.ProductColorDto;
import JOO.jooshop.product.model.ProductDetailResponseDto;
import JOO.jooshop.product.model.ProductListResponseDto;
import JOO.jooshop.product.model.ProductRequestDto;
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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static JOO.jooshop.global.exception.ResponseMessageConstants.PRODUCT_NOT_FOUND;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
@Slf4j
public class ProductServiceV1 {

    private final ProductRepositoryV1 productRepository;
    private final ProductColorRepositoryV1 productColorRepository;
    private final ModelMapper modelMapper;
    private final ThumbnailService thumbnailService;
    private final ContentImgService contentImgService;
    private final ProductRankingService productRankingService;

    /**
     * 상품 등록 (MultipartFile 반영)
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public Long createProduct(ProductRequestDto requestDto,
                              @Nullable MultipartFile thumbnail,
                              @Nullable List<MultipartFile> contentImages,
                              UploadType uploadType) {

        Product product = new Product(requestDto);
        productRepository.save(product);

        // 썸네일 업로드
        if (thumbnail != null && !thumbnail.isEmpty()) {
            thumbnailService.uploadThumbnailImages(product, thumbnail);
        }

        // 상세 이미지 업로드
        if (contentImages != null && !contentImages.isEmpty()) {
            contentImgService.uploadContentImages(product, contentImages, uploadType);
        }

        return product.getProductId();
    }

    /**
     * 상품 수정 (MultipartFile 반영)
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public ProductDetailResponseDto updateProduct(Long productId,
                                                  ProductRequestDto updatedDto,
                                                  @Nullable MultipartFile thumbnail,
                                                  @Nullable List<MultipartFile> contentImages) {

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND));

        existingProduct.updateFromRequestDto(updatedDto);
        productRepository.save(existingProduct);

        // 썸네일 및 상세 이미지 업데이트
        if (thumbnail != null && !thumbnail.isEmpty()) {
            thumbnailService.uploadThumbnailImages(existingProduct, thumbnail);
        }

        if (contentImages != null && !contentImages.isEmpty()) {
            contentImgService.uploadContentImages(existingProduct, contentImages, UploadType.PRODUCT);
        }

        return new ProductDetailResponseDto(existingProduct);
    }

    /**
     * 상품 목록 조회(전체)
     */
    @Transactional(readOnly = true)
    public List<ProductListResponseDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductListResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 상품 상세 조회
     */
    @Transactional(readOnly = true)
    public ProductDetailResponseDto productDetail(Long productId) {
        Product product = productRepository.findProductWithDetailsByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND));

        productRankingService.increaseProductViews(productId);

        ProductDetailResponseDto dto = new ProductDetailResponseDto(product);

        List<ProductManagement> options = product.getProductManagements();
        if (!options.isEmpty()) {
            dto.withInventoryId(options.get(0).getInventoryId());
        }

        return dto;
    }

    /**
     * 상품 삭제
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND));
        productRepository.delete(product);
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
