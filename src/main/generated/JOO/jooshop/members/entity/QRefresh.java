package JOO.jooshop.members.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRefresh is a Querydsl query type for Refresh
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRefresh extends EntityPathBase<Refresh> {

    private static final long serialVersionUID = 338222039L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRefresh refresh = new QRefresh("refresh");

    public final DateTimePath<java.time.LocalDateTime> expiration = createDateTime("expiration", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QMember member;

    public final StringPath refreshToken = createString("refreshToken");

    public QRefresh(String variable) {
        this(Refresh.class, forVariable(variable), INITS);
    }

    public QRefresh(Path<? extends Refresh> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRefresh(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRefresh(PathMetadata metadata, PathInits inits) {
        this(Refresh.class, metadata, inits);
    }

    public QRefresh(Class<? extends Refresh> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new QMember(forProperty("member")) : null;
    }

}

