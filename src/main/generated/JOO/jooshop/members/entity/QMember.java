package JOO.jooshop.members.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMember is a Querydsl query type for Member
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMember extends EntityPathBase<Member> {

    private static final long serialVersionUID = -132042210L;

    public static final QMember member = new QMember("member1");

    public final BooleanPath accountExpired = createBoolean("accountExpired");

    public final BooleanPath active = createBoolean("active");

    public final ListPath<JOO.jooshop.address.entity.Addresses, JOO.jooshop.address.entity.QAddresses> addresses = this.<JOO.jooshop.address.entity.Addresses, JOO.jooshop.address.entity.QAddresses>createList("addresses", JOO.jooshop.address.entity.Addresses.class, JOO.jooshop.address.entity.QAddresses.class, PathInits.DIRECT2);

    public final BooleanPath admin = createBoolean("admin");

    public final BooleanPath banned = createBoolean("banned");

    public final BooleanPath certifiedByEmail = createBoolean("certifiedByEmail");

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> joinedAt = createDateTime("joinedAt", java.time.LocalDateTime.class);

    public final EnumPath<JOO.jooshop.members.entity.enums.MemberRole> memberRole = createEnum("memberRole", JOO.jooshop.members.entity.enums.MemberRole.class);

    public final StringPath nickname = createString("nickname");

    public final StringPath password = createString("password");

    public final BooleanPath passwordExpired = createBoolean("passwordExpired");

    public final ListPath<JOO.jooshop.payment.entity.PaymentHistory, JOO.jooshop.payment.entity.QPaymentHistory> paymentHistories = this.<JOO.jooshop.payment.entity.PaymentHistory, JOO.jooshop.payment.entity.QPaymentHistory>createList("paymentHistories", JOO.jooshop.payment.entity.PaymentHistory.class, JOO.jooshop.payment.entity.QPaymentHistory.class, PathInits.DIRECT2);

    public final StringPath phoneNumber = createString("phoneNumber");

    public final StringPath socialId = createString("socialId");

    public final EnumPath<JOO.jooshop.members.entity.enums.SocialType> socialType = createEnum("socialType", JOO.jooshop.members.entity.enums.SocialType.class);

    public final StringPath username = createString("username");

    public final ListPath<JOO.jooshop.wishList.entity.WishList, JOO.jooshop.wishList.entity.QWishList> wishLists = this.<JOO.jooshop.wishList.entity.WishList, JOO.jooshop.wishList.entity.QWishList>createList("wishLists", JOO.jooshop.wishList.entity.WishList.class, JOO.jooshop.wishList.entity.QWishList.class, PathInits.DIRECT2);

    public QMember(String variable) {
        super(Member.class, forVariable(variable));
    }

    public QMember(Path<? extends Member> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMember(PathMetadata metadata) {
        super(Member.class, metadata);
    }

}

