package JOO.jooshop.global.pagination.util;

import JOO.jooshop.global.pagination.model.PaginationResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PageUtils {

    /**
     * Spring Data Page 객체 -> PaginationResponse 변환
     */
    public <T> PaginationResponse<T> toResponse(Page<T> pageData) {

        int page = pageData.getNumber() + 1; // 0 -> 1 변환
        int totalPages = pageData.getTotalPages();

        // 화면에 5개씩만 페이지 띄우기
        int pageGroupSize = 5;

        int startPage = ((page -1) / pageGroupSize) * pageGroupSize + 1;
        int endPage = Math.min(startPage + pageGroupSize -1, totalPages);

        return PaginationResponse.<T>builder()
                .content(pageData.getContent())
                .page(page)
                .size(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPages(totalPages)
                .hasNext(pageData.hasNext())
                .hasPrev(pageData.hasPrevious())
                .startPage(startPage)
                .endPage(endPage)
                .build();
    }
}
