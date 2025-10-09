package JOO.jooshop.product.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProduct is a Querydsl query type for Product
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProduct extends EntityPathBase<Product> {

    private static final long serialVersionUID = -743560363L;

    public static final QProduct product = new QProduct("product");

    public final ListPath<JOO.jooshop.contentImgs.entity.ContentImages, JOO.jooshop.contentImgs.entity.QContentImages> contentImages = this.<JOO.jooshop.contentImgs.entity.ContentImages, JOO.jooshop.contentImgs.entity.QContentImages>createList("contentImages", JOO.jooshop.contentImgs.entity.ContentImages.class, JOO.jooshop.contentImgs.entity.QContentImages.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> discountRate = createNumber("discountRate", Integer.class);

    public final BooleanPath isDiscount = createBoolean("isDiscount");

    public final BooleanPath isRecommend = createBoolean("isRecommend");

    public final StringPath manufacturer = createString("manufacturer");

    public final NumberPath<java.math.BigDecimal> price = createNumber("price", java.math.BigDecimal.class);

    public final NumberPath<Long> productId = createNumber("productId", Long.class);

    public final StringPath productInfo = createString("productInfo");

    public final ListPath<JOO.jooshop.productManagement.entity.ProductManagement, JOO.jooshop.productManagement.entity.QProductManagement> productManagements = this.<JOO.jooshop.productManagement.entity.ProductManagement, JOO.jooshop.productManagement.entity.QProductManagement>createList("productManagements", JOO.jooshop.productManagement.entity.ProductManagement.class, JOO.jooshop.productManagement.entity.QProductManagement.class, PathInits.DIRECT2);

    public final StringPath productName = createString("productName");

    public final ListPath<JOO.jooshop.productThumbnail.entity.ProductThumbnail, JOO.jooshop.productThumbnail.entity.QProductThumbnail> productThumbnails = this.<JOO.jooshop.productThumbnail.entity.ProductThumbnail, JOO.jooshop.productThumbnail.entity.QProductThumbnail>createList("productThumbnails", JOO.jooshop.productThumbnail.entity.ProductThumbnail.class, JOO.jooshop.productThumbnail.entity.QProductThumbnail.class, PathInits.DIRECT2);

    public final EnumPath<JOO.jooshop.product.entity.enums.ProductType> productType = createEnum("productType", JOO.jooshop.product.entity.enums.ProductType.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> wishListCount = createNumber("wishListCount", Long.class);

    public final ListPath<JOO.jooshop.wishList.entity.WishList, JOO.jooshop.wishList.entity.QWishList> wishLists = this.<JOO.jooshop.wishList.entity.WishList, JOO.jooshop.wishList.entity.QWishList>createList("wishLists", JOO.jooshop.wishList.entity.WishList.class, JOO.jooshop.wishList.entity.QWishList.class, PathInits.DIRECT2);

    public QProduct(String variable) {
        super(Product.class, forVariable(variable));
    }

    public QProduct(Path<? extends Product> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProduct(PathMetadata metadata) {
        super(Product.class, metadata);
    }

}

