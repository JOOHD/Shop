package JOO.jooshop.global.dummydata.util;

import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.categorys.repository.CategoryRepository;
import JOO.jooshop.categorys.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class CategoryDataUtil {

    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    public List<Category> generateCategoryData() {
        List<Category> categories = new ArrayList<>();

        // top, bottom, dress&set, outer, shoes, accessories 순서
        // "상의"에 대한 하위 카테고리 생성 후 , 하위 카테고리로 다음을 추가
        List<Category> topsChildren = new ArrayList<>();
        Category topParentCategory = createAndSaveParentCategory(1L, "top", new ArrayList<>());
        Category blouse = createAndSaveChildCategoryV2(2L, "blouse", topParentCategory.getCategoryId(), new ArrayList<>());
        topsChildren.add(blouse);
        Category hoodie = createAndSaveChildCategoryV2(3L, "hoodie", topParentCategory.getCategoryId(), new ArrayList<>());
        topsChildren.add(hoodie);
        Category knitSweater = createAndSaveChildCategoryV2(4L, "knit-sweater", topParentCategory.getCategoryId(), new ArrayList<>());
        topsChildren.add(knitSweater);
        Category longShirts = createAndSaveChildCategoryV2(5L, "long-shirts", topParentCategory.getCategoryId(), new ArrayList<>());
        topsChildren.add(longShirts);
        Category longSleeve = createAndSaveChildCategoryV2(6L, "long-sleeve", topParentCategory.getCategoryId(), new ArrayList<>());
        topsChildren.add(longSleeve);
        Category shortShirts = createAndSaveChildCategoryV2(7L, "short-shirts", topParentCategory.getCategoryId(), new ArrayList<>());
        topsChildren.add(shortShirts);
        Category shortSleeve = createAndSaveChildCategoryV2(8L, "short-sleeve", topParentCategory.getCategoryId(), new ArrayList<>());
        topsChildren.add(shortSleeve);
        Category sweatshirt = createAndSaveChildCategoryV2(9L, "sweatshirt", topParentCategory.getCategoryId(), new ArrayList<>());
        topsChildren.add(sweatshirt);
        topParentCategory.setChildren(topsChildren);

        // "하의"에 하위 카테고리 생성
        List<Category> bottomChildren = new ArrayList<>();
        Category bottomParentCategory = createAndSaveParentCategory(10L, "bottom", new ArrayList<>());
        Category longBottom = createAndSaveChildCategoryV2(11L, "long", bottomParentCategory.getCategoryId(), new ArrayList<>());
        bottomChildren.add(longBottom);
        Category shorts = createAndSaveChildCategoryV2(12L, "shorts", bottomParentCategory.getCategoryId(), new ArrayList<>());
        bottomChildren.add(shorts);
        Category skirt = createAndSaveChildCategoryV2(13L, "skirt", bottomParentCategory.getCategoryId(), new ArrayList<>());
        bottomChildren.add(skirt);
        bottomParentCategory.setChildren(bottomChildren);

        // "dress&set"에 하위 카테고리 생성
        List<Category> dressChildren = new ArrayList<>();
        Category dressParentCategory = createAndSaveParentCategory(14L, "dress&set", new ArrayList<>());
        Category dress = createAndSaveChildCategoryV2(15L, "dress", dressParentCategory.getCategoryId(), new ArrayList<>());
        dressChildren.add(dress);
        Category setUp = createAndSaveChildCategoryV2(16L, "set-up", dressParentCategory.getCategoryId(), new ArrayList<>());
        dressChildren.add(setUp);
        Category twoPiece = createAndSaveChildCategoryV2(17L, "two-piece", dressParentCategory.getCategoryId(), new ArrayList<>());
        dressChildren.add(twoPiece);
        dressParentCategory.setChildren(dressChildren);

        // "아우터"에 대한 하위 카테고리 생성 후, 하위 카테고리로 다음을 추가
        List<Category> outerChildren = new ArrayList<>();
        Category outerParentCategory = createAndSaveParentCategory(18L, "outer", new ArrayList<>());
        Category cardigan = createAndSaveChildCategoryV2(19L, "cardigan", outerParentCategory.getCategoryId(), new ArrayList<>());
        outerChildren.add(cardigan);
        Category coat = createAndSaveChildCategoryV2(20L, "coat", outerParentCategory.getCategoryId(), new ArrayList<>());
        outerChildren.add(coat);
        Category jacket = createAndSaveChildCategoryV2(21L, "jacket", outerParentCategory.getCategoryId(), new ArrayList<>());
        outerChildren.add(jacket);
        Category lightweightPadding = createAndSaveChildCategoryV2(22L, "lightweight-padding", outerParentCategory.getCategoryId(), new ArrayList<>());
        outerChildren.add(lightweightPadding);
        Category longPadding = createAndSaveChildCategoryV2(23L, "long-padding", outerParentCategory.getCategoryId(), new ArrayList<>());
        outerChildren.add(longPadding);
        Category mustang = createAndSaveChildCategoryV2(24L, "mustang", outerParentCategory.getCategoryId(), new ArrayList<>());
        outerChildren.add(mustang);
        Category shortPadding = createAndSaveChildCategoryV2(25L, "short-padding", outerParentCategory.getCategoryId(), new ArrayList<>());
        outerChildren.add(shortPadding);
        Category vest = createAndSaveChildCategoryV2(26L, "vest", outerParentCategory.getCategoryId(), new ArrayList<>());
        outerChildren.add(vest);
        outerParentCategory.setChildren(outerChildren);

        // "신발"에 대한 하위 카테고리 생성 후, 하위 카테고리로 다음을 추가
        List<Category> shoesChildren = new ArrayList<>();
        Category shoesParentCategory = createAndSaveParentCategory(27L, "shoes", new ArrayList<>());
        Category boots = createAndSaveChildCategoryV2(28L, "boots", shoesParentCategory.getCategoryId(), new ArrayList<>());
        shoesChildren.add(boots);
        Category sandal = createAndSaveChildCategoryV2(29L, "sandal", shoesParentCategory.getCategoryId(), new ArrayList<>());
        shoesChildren.add(sandal);
        Category sneakers = createAndSaveChildCategoryV2(30L, "sneakers", shoesParentCategory.getCategoryId(), new ArrayList<>());
        shoesChildren.add(sneakers);
        shoesParentCategory.setChildren(shoesChildren);

        // "악세사리"에 대한 하위 카테고리 생성 후, 하위 카테고리로 다음을 추가
        List<Category> accessoryChildren = new ArrayList<>();
        Category accessoryParentCategory = createAndSaveParentCategory(31L, "accessories", new ArrayList<>());
        Category bag = createAndSaveChildCategoryV2(32L, "bag", accessoryParentCategory.getCategoryId(), new ArrayList<>());
        accessoryChildren.add(bag);
        Category cap = createAndSaveChildCategoryV2(33L, "cap", accessoryParentCategory.getCategoryId(), new ArrayList<>());
        accessoryChildren.add(cap);
        Category socks = createAndSaveChildCategoryV2(34L, "socks", accessoryParentCategory.getCategoryId(), new ArrayList<>());
        accessoryChildren.add(socks);
        accessoryParentCategory.setChildren(accessoryChildren);

        // Add constructed parent categories to categories list
        categories.addAll(topParentCategory.getChildren());
        categories.addAll(bottomParentCategory.getChildren());
        categories.addAll(dressParentCategory.getChildren());
        categories.addAll(outerParentCategory.getChildren());
        categories.addAll(shoesParentCategory.getChildren());
        categories.addAll(accessoryParentCategory.getChildren());

        System.out.println("categories = " + categories);
        return categories;
    }


    private Category createAndSaveParentCategory(Long categoryId, String name, List<Category> children) {
        // DB 에 이미 존재하는 category Id 인지 확인한다.
        Optional<Category> optionalCategory = categoryRepository.findByCategoryId(categoryId);

        if (optionalCategory.isPresent()) {
            return optionalCategory.get();
        } else {
            Category category = Category.createCategoryDummyData(categoryId, name, 0L, children);
            children.forEach(child -> child.setParent(category));
            category.setChildren(children);
            categoryService.createCategory(category, null);
            return category;
        }
    }

    protected Category createAndSaveChildCategoryV2(Long categoryId, String name, Long parentId, List<Category> children) {
        // DB에 이미 존재하는 category Id인지 확인한다.
        Optional<Category> optionalCategory = categoryRepository.findByCategoryId(categoryId);

        if(optionalCategory.isPresent()){
            // If category already exists, return it
            return optionalCategory.get();
        } else {
            Category category = Category.createChildCategory(categoryId, name, 1L, children);

            if (parentId != null) {
                Category parentCategory = categoryRepository.findById(parentId).orElseThrow(() ->
                        new RuntimeException("부모 카테고리를 찾을 수 없습니다. ID: " + parentId));
                category.setParent(parentCategory);
            }

            categoryService.createCategory(category, parentId);
            return category;
        }
    }


    /**
     * 2024.04.23 김성우
     * 부모 카테고리 생성 후, 자식 카테고리를 생성하지 않아
     * 부모 카테고리 id를 찾을 수 없어 RuntimeException 발생
     */
    public void generateCategoryDataV1() {
        // "상의"에 대한 하위 카테고리 생성 후 , 하위 카테고리로 다음을 추가
        List<Category> TopChildren = new ArrayList<>();
        TopChildren.add(createAndSaveChildCategoryV2(2L, "후드", 1L, new ArrayList<>()));
        TopChildren.add(createAndSaveChildCategoryV2(3L, "맨투맨", 1L,new ArrayList<>()));
        TopChildren.add(createAndSaveChildCategoryV2(4L, "반팔 셔츠", 1L,new ArrayList<>()));
        TopChildren.add(createAndSaveChildCategoryV2(5L, "긴팔 셔츠", 1L,new ArrayList<>()));
        TopChildren.add(createAndSaveChildCategoryV2(6L, "반팔티", 1L,new ArrayList<>()));
        TopChildren.add(createAndSaveChildCategoryV2(7L, "긴팔티", 1L,new ArrayList<>()));
        TopChildren.add(createAndSaveChildCategoryV2(8L, "니트/스웨터", 1L,new ArrayList<>()));
        TopChildren.add(createAndSaveChildCategoryV2(9L, "블라우스", 1L,new ArrayList<>()));
        createAndSaveParentCategory(1L, "상의", TopChildren);

        // "하의"에 대한 하위 카테고리 생성 후 , 하위 카테고리로 다음을 추가
        List<Category> bottomChildren = new ArrayList<>();
        bottomChildren.add(createAndSaveChildCategoryV2(11L, "긴바지", 10L, new ArrayList<>()));
        bottomChildren.add(createAndSaveChildCategoryV2(12L, "반바지", 10L, new ArrayList<>()));
        bottomChildren.add(createAndSaveChildCategoryV2(13L, "치마", 10L, new ArrayList<>()));
        createAndSaveParentCategory(10L, "하의", bottomChildren);

        // "dress&set"에 대한 하위 카테고리 생성 후 , 하위 카테고리로 다음을 추가
        List<Category> setChildren = new ArrayList<>();
        setChildren.add(createAndSaveChildCategoryV2(15L, "원피스", 14L, new ArrayList<>()));
        setChildren.add(createAndSaveChildCategoryV2(16L, "투피스", 14L, new ArrayList<>()));
        setChildren.add(createAndSaveChildCategoryV2(17L, "셋업", 14L, new ArrayList<>()));
        createAndSaveParentCategory(14L, "dress&set", setChildren);

        // "아우터"에 대한 하위 카테고리 생성 후 , 하위 카테고리로 다음을 추가
        List<Category> outerChildren = new ArrayList<>();
        outerChildren.add(createAndSaveChildCategoryV2(19L, "숏패딩", 18L, new ArrayList<>()));
        outerChildren.add(createAndSaveChildCategoryV2(20L, "롱패딩", 18L, new ArrayList<>()));
        outerChildren.add(createAndSaveChildCategoryV2(21L, "가디건", 18L, new ArrayList<>()));
        outerChildren.add(createAndSaveChildCategoryV2(22L, "재킷", 18L, new ArrayList<>()));
        outerChildren.add(createAndSaveChildCategoryV2(23L, "코트", 18L, new ArrayList<>()));
        outerChildren.add(createAndSaveChildCategoryV2(24L, "무스탕", 18L, new ArrayList<>()));
        outerChildren.add(createAndSaveChildCategoryV2(25L, "조끼", 18L, new ArrayList<>()));
        outerChildren.add(createAndSaveChildCategoryV2(26L, "경량패딩", 18L, new ArrayList<>()));
        createAndSaveParentCategory(18L, "아우터", outerChildren);

        // "신발"에 대한 하위 카테고리 생성 후 , 하위 카테고리로 다음을 추가
        List<Category> shoesChildren = new ArrayList<>();
        shoesChildren.add(createAndSaveChildCategoryV2(28L, "스니커즈", 27L, new ArrayList<>()));
        shoesChildren.add(createAndSaveChildCategoryV2(29L, "샌들/슬리퍼", 27L, new ArrayList<>()));
        shoesChildren.add(createAndSaveChildCategoryV2(30L, "부츠", 27L, new ArrayList<>()));
        createAndSaveParentCategory(27L, "신발", shoesChildren);

        // "악세서리"에 대한 하위 카테고리 생성 후 , 하위 카테고리로 다음을 추가
        List<Category> accessoryChildren = new ArrayList<>();
        accessoryChildren.add(createAndSaveChildCategoryV2(32L, "모자", 31L, new ArrayList<>()));
        accessoryChildren.add(createAndSaveChildCategoryV2(33L, "양말", 31L, new ArrayList<>()));
        accessoryChildren.add(createAndSaveChildCategoryV2(34L, "가방", 31L, new ArrayList<>()));
        createAndSaveParentCategory(31L, "악세사리", accessoryChildren);
    }
}
