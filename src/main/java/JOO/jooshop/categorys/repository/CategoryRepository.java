package JOO.jooshop.categorys.repository;

import JOO.jooshop.categorys.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 카테고리 ID로 조회
    Optional<Category> findByCategoryId(Long categoryId);

    // 부모 카테고리가 없는 최상위 카테고리 조회
    List<Category> findByParentIsNull();

    Optional<Category> findByName(String name);

}
