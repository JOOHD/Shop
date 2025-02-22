package JOO.jooshop.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE}) // 적용 대상, Element(enum, annotation, interface,, class level)
@Retention(RetentionPolicy.RUNTIME) // annotation runtime 유지 시간
@Constraint(validatedBy = DiscountRateValidator.class) // 유효성 검사를 수행할 클래스
public @interface ValidDiscountRate {

    // 커스텀 유효성 검사 애노테이션 - 할인

    String message() default "할인이 적용되면 할인율을 입력해야 하며, 할인이 적용되지 않으면 할인율을 입력할 수 업습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
