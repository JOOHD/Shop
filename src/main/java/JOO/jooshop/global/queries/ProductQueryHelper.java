package JOO.jooshop.global.queries;

import JOO.jooshop.product.entity.QProduct;
import JOO.jooshop.product.entity.enums.ProductType;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;

import java.time.LocalDateTime;

public class ProductQueryHelper {

    /**
     * 정렬 수행
     * @param order 정렬 조건
     * @param product
     * @return
     */
    public static OrderSpecifier<?> getOrderSpecifier(OrderBy order, QProduct product) {
        if (order == null) {
            // order 가 null 인 경우 기본 정렬 기준으로 처리
            return product.createdAt.desc();
        }
        switch (order) {
            case LATEST:
                return product.createdAt.desc();
            case POPULAR:
                return product.wishListCount.desc();
            case LOW_PRICE:
                return product.price.asc();
            case HIGH_PRICE:
                return product.price.desc();
            case HIGH_DISCOUNT_RATE:
                return product.discountRate.desc();
            default:
                return product.createdAt.desc();
        }

    }

    /**
     * 필터링 수행
     * @param condition
     * @param category
     * @param keyword
     * @return
     */
    public static BooleanBuilder createFilterBuilder(Condition condition, Long category, String keyword, QProduct product) {
        //
        BooleanBuilder filterBuilder = new BooleanBuilder();

        // 조건 필터링
        addConditionFilters(condition, product, filterBuilder);
        // 카테고리 필터링
        addCategoryFilter(category, product, filterBuilder);
        // 검색
        addKeywordFilter(keyword, product, filterBuilder);

        return filterBuilder;
    }

    // 조건 필터링 메서드
    private static void addConditionFilters(Condition condition, QProduct product, BooleanBuilder filterBuilder) {
        if (condition != null) {
            switch (condition) {
                case NEW:
                    filterBuilder.and(product.createdAt.after(LocalDateTime.now().minusMonths(1)));
                    break;
                case BEST:
                    filterBuilder.and(product.wishListCount.goe(30L));
                    break;
                case DISCOUNT:
                    filterBuilder.and(product.isDiscount.isTrue());
                    break;
                case RECOMMEND:
                    filterBuilder.and(product.isRecommend.isTrue());
                    break;
                case MAN:
                case WOMAN:
                case UNISEX:
                    filterBuilder.and(product.productType.eq(ProductType.valueOf(condition.name())));
                    break;
                default:
                    filterBuilder.and(product.createdAt.after(LocalDateTime.now().minusMonths(1)));
                    break;
            }
        }
    }

    // 카테고리 필터링 메서드
    private static void addCategoryFilter(Long category, QProduct product, BooleanBuilder filterBuilder) {
        if (category != null) {
            filterBuilder.andAnyOf(
                    product.productManagements.any().category.categoryId.eq(category),
                    product.productManagements.any().category.parent.categoryId.eq(category)
            );
        }
    }

    // 검색 메서드
    private static void addKeywordFilter(String keyword, QProduct product, BooleanBuilder filterBuilder) {
        if (keyword != null) {
            filterBuilder.and(
                    product.productName.containsIgnoreCase(keyword)
                            .or(product.productInfo.containsIgnoreCase(keyword))
            );
        }
    }
}
