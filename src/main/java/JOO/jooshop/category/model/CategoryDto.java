package JOO.jooshop.category.model;

import JOO.jooshop.category.entity.Category;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(of = "categoryId")
@AllArgsConstructor
public class CategoryDto {
    private Long categoryId;
    @NotNull(message = "카테고리 이름은 필수입니다.")
    private String name;
    private Long depth;
    private List<CategoryDto> children;

    public static CategoryDto of (Category category) {
        return new CategoryDto(
                category.getCategoryId(),
                category.getName(),
                category.getDepth(),
                category.getChildren().stream().map(CategoryDto::of).collect(Collectors.toList())
        );

    }
}
