package JOO.jooshop.wishList.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QWishList is a Querydsl query type for WishList
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QWishList extends EntityPathBase<WishList> {

    private static final long serialVersionUID = 401767385L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QWishList wishList = new QWishList("wishList");

    public final JOO.jooshop.members.entity.QMember member;

    public final JOO.jooshop.product.entity.QProduct product;

    public final NumberPath<Long> wishListId = createNumber("wishListId", Long.class);

    public QWishList(String variable) {
        this(WishList.class, forVariable(variable), INITS);
    }

    public QWishList(Path<? extends WishList> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QWishList(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QWishList(PathMetadata metadata, PathInits inits) {
        this(WishList.class, metadata, inits);
    }

    public QWishList(Class<? extends WishList> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new JOO.jooshop.members.entity.QMember(forProperty("member")) : null;
        this.product = inits.isInitialized("product") ? new JOO.jooshop.product.entity.QProduct(forProperty("product")) : null;
    }

}

