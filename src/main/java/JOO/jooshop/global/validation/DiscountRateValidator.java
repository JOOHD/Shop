package JOO.jooshop.global.validation;

import JOO.jooshop.product.model.ProductCreateDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DiscountRateValidator implements ConstraintValidator<ValidDiscountRate, ProductCreateDto> {
    @Override
    public boolean isValid(ProductCreateDto value, ConstraintValidatorContext context) {
        // isDiscount 가 true && discountRate 가 null || 0 보다 작으면 유효하지 않음
        if (value.getIsDiscount() && (value.getDiscountRate() == null || value.getDiscountRate() < 1)) {
            return false;
        }
        // isDiscount 가 false, discountRate 가 null 이 아니면 유효하지 않음
        if (value.getIsDiscount() && value.getDiscountRate() != null) {
            return false;
        }
        return true;
    }
}
