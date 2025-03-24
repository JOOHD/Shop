package JOO.jooshop.Inquiry.validation;

import JOO.jooshop.Inquiry.entity.enums.InquiryType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EnumValidator implements ConstraintValidator<EnumConstraint, Enum<?>> {

    // 커스텀 제약 조건 정의 & 검증 로직 처리, 주로 어노테이션 검증을 위함.
    // @EnumConstraint 가 붙은 필드에 대해 검사를 수행.
    @Override
    public boolean isValid(Enum<?> value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        try {
            // Check if the enum value is contained in the defined enum constants
            for (InquiryType enumValue : InquiryType.values()) {
                if (enumValue.equals(value)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }
}
