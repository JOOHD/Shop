package JOO.jooshop.categorys.service;

import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.categorys.model.CategoryDto;
import JOO.jooshop.categorys.repository.CategoryRepository;
import JOO.jooshop.global.authorization.RequiresRole;
import JOO.jooshop.members.entity.enums.MemberRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository  categoryRepository;

    /**
     * 최상위 카테고리 조회
     * @return
     */
    public List<Category> getTopLevelCategories() {
        return categoryRepository.findByParentIsNull();
    }

    /**
     * 모든 카테고리 조회 (DTO 변환)
     * @return
     */
    public List<CategoryDto> getCategoryList() {
        return categoryRepository.findAll().stream()
                .map(CategoryDto::of)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 생성
     * @param request 카테고리 정보
     * @param parentId 부모 카테고리 ID (null인 경우 최상위 카테고리)
     * @return 생성된 카테고리 ID
     *
     * 부모 카테고리 ID = Null, 최상위 카테고리 생성
     * 부모 카테고리 ID != Null, 해당 부모 카테고리 하위에 자식 카테고리 생성
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public Long createCategory(Category request, Long parentId) {
        Category category = parentId != null ? createChildCategory(request, parentId) : createTopLevelCategory(request);
        return categoryRepository.save(category).getCategoryId();
    }

    /**
     * 최상위 카테고리 생성
     */
    private Category createTopLevelCategory(Category request) {
        return new Category(0L, request.getName());
    }

    /**
     * 자식 카테고리 생성
     * @param request 카테고리 정보
     * @param parentId 부모 카테고리 ID
     * @return 자식 카테고리
     *
     * 부모 카테고리 depth = 0, 자식 카테고리를 만들 수 있으며, 이를 통해 계층 구조를 유지
     */
    private Category createChildCategory(Category request, Long parentId) {
        Category parentCategory = categoryRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 카테고리를 찾을 수 없습니다. Id : " + parentId));

        // 부모 카테고리 depth = 0, 경우만 자식 카테고리 생성 가능
        if (parentCategory.getDepth() != 0L) {
            throw new IllegalArgumentException("올바른 상위 카테고리를 입력하세요.");
        }

        Category childCategory = new Category(parentCategory, parentCategory.getDepth() + 1, request.getName());
        parentCategory.getChildren().add(childCategory); // 자식 카테고리 추가
        return childCategory;
    }

    /**
     * 카테고리 삭제
     * @param categoryId 카테고리 ID
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 카테고리를 찾을 수 없습니다. Id : " + categoryId));

        if (!category.getChildren().isEmpty()) {
            throw new IllegalArgumentException("삭제 실패 : 하위 카테고리가 존재합니다.");
        }

        categoryRepository.delete(category);
    }
}

/*
    * Ver1
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public Long createCategory(Category request, Long parentId) {
        Category category;
        if (parentId != null) {
            // 부모 카테고리가 지정된 경우
            Category parentCategory = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new NoSuchElementException("해당 카테고리를 찾을 수 없습니다."));
            if (parentCategory.getDepth() != 0L) {
                // 입력한 parentId 가 상위 카테고리의 id 가 아닌 하위 카테고리의 id 일 경우 에러 반환
                throw new InvalidParameterException("올바른 상위 카테고리를 입력하세요.");
            }

            category = new Category(parentCategory, parentCategory.getDepth() + 1, request.getName());

            parentCategory.getChildren().add(category);
        } else {
            // 부모 카테고리가 지정되지 않은 경우 최상위 카테고리로 설정
            category = new Category(0L, request.getName());
        }

        Category savedCategory = categoryRepository.save(category);
        return savedCategory.getCategoryId();
    }
*/
