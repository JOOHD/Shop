package JOO.jooshop.payment.entity;

public enum PaymentStatus {

    // 상태 코드(COMPLETE, CANCELED) + 설명 텍스트("완료", "취소")를 한 번에 관리하는 구조

    COMPLETE("완료"),
    CANCELED("취소");

    private final String description; // 상태에 대한 한글 무나열 (불변)

    PaymentStatus(String description) { // 상수 생성 시, 한 번 설정
        this.description = description;
    }

    public String getDescription() { // 설정된 한글 설명을 외부에 제공하는 Getter 메서드.
        return description;
    }
}
