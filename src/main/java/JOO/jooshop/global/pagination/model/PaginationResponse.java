package JOO.jooshop.global.pagination.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PaginationResponse<T> {

    private List<T> content; // 현재 페이지 데이터
    private int page;        // 현재 페이지 번호
    private int size;        // 요청한 size
    private long totalElements; // 전체 데이터 개수
    private int totalPages;     // 전체 페이지 개수

    private boolean hasNext; // 다음 페이지 여부
    private boolean hasPrev; // 이전 페이지 여부

    private int startPage;   // UI용 시작 페이지
    private int endPage;     // UI용 마지막 페이지
}
