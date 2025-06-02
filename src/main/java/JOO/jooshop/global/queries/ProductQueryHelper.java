package JOO.jooshop.global.queries;

import JOO.jooshop.product.entity.QProduct;
import JOO.jooshop.product.entity.enums.ProductType;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;

import java.time.LocalDateTime;

public class ProductQueryHelper {
    /*
        QueryHelper
        1. 동적 조건(where절) 만들기 쉽게하기 위해
        2. BooleanBuilder 같은 로직을 모듈화해서 반복 코드 줄임
        3. 페이징/정렬 같은 공통 기능을 재사용 위해

        해당 클래스는 개발자가 생성한 클래스이다.
     */

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
        // 동적 WHERE 절 처리용. 조건이 들어오면 .and()로 계속 붙인다.
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

    /*
        BooleanBuilder filterBuilder = new BooleanBuilder();

        1. 조건 필터 (예: 신규 상품)
          AND
        2. 카테고리 필터 (예: 카테고리 id = 1 인 상품 OR 부모 카테고리 id = 1)
          AND
        3. 키워드 검색 필터 (예: 상품명에 "신발" 포함 OR 상품설명에 "신발" 포함)
        모든 조건이 AND로 조합돼서 최종적으로 where 절에 들어가게 되는 거야.
     */

    // 카테고리 필터링 메서드
    private static void addCategoryFilter(Long category, QProduct product, BooleanBuilder filterBuilder) {
        if (category != null) {
            filterBuilder.andAnyOf( // andAnyOf() 여러 조건 중 하나라도 만족 시, true 그러나 filterBuilder AND 로 계속 추가.
                    // QProduct.managements.any(리스트 요건 중 하나라도 만족 시, true)....equals(data)
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
