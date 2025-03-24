package JOO.jooshop.Inquiry.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented // 문서화 -> Javadoc 포함
@Constraint(validatedBy = EnumValidator.class)
@Target({ElementType.FIELD}) // 어노테이션을 필드에서만 사용
@Retention(RetentionPolicy.RUNTIME) // 런타임에도 유지되도록
public @interface EnumConstraint {

    // 검증하고자 하는 enum 타입에 대해 검증을 수행.
    // EnumValidator 클래스를 참조하여 검증 로직 실행
    String message() default "Invalid enum value"; // 검증 실패 시
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
