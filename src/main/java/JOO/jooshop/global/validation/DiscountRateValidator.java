package JOO.jooshop.global.validation;

import JOO.jooshop.product.model.ProductCreateDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DiscountRateValidator implements ConstraintValidator<ValidDiscountRate, ProductCreateDto> {
    @Override
    public boolean isValid(ProductCreateDto value, ConstraintValidatorContext context) {
        // if (value.getIsDiscount() && value.getDiscountRate() != null) 조건이 잘못 들어가 있었다.
        // 할인 적용 중이라면 discountRate는 1 이상 100 이하여야 함
        if (value.getIsDiscount()) {
            return  value.getDiscountRate() != null &&
                    value.getDiscountRate() >= 1 &&
                    value.getDiscountRate() <= 100;
        } else {
          return value.getDiscountRate() == null;
        }
    }
}
