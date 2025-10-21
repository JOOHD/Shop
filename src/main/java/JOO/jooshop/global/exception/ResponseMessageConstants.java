package JOO.jooshop.global.exception;

public class ResponseMessageConstants {
    public static final String PRODUCT_NOT_FOUND = "상품을 찾을 수 없습니다.";
    public static final String MEMBER_NOT_FOUND = "회원을 찾을 수 없습니다.";
    public static final String ORDER_NOT_FOUND = " 주문을 찾을 수 없습니다.";
    public static final String CART_NOT_FOUND = "장바구니를 찾을 수 없습니다.";
    public static final String WRITING_NOT_FOUND = "글을 찾을 수 없습니다.";

    public static final String DELETE_SUCCESS = "삭제되었습니다.";
    public static final String ACCESS_DENIED = "접근 권한이 없습니다.";
    public static final String EMAIL_ALREADY_EXISTS = "이미 가입된 이메일입니다.";
    public static final String MEMBER_ALREADY_EXISTS = "이미 존재하는 회원입니다.";
    public static final String INVALID_NICKNAME = "닉네임을 입력해주세요";
    public static final String ACCESS_DENIED_NO_AUTHENTICATION = "접근 권한이 없습니다. : 로그인 정보 찾을 수 없음";
    public static final String ADDRESS_NOT_FOUND = "해당 주소를 찾을 수 없습니다.";
    public static final String PAYMENT_HISTORY_NOT_FOUND = "결제 내역을 찾을 수 없습니다.";  // 25.03.12
    public static final String PAYMENT_HISTORY_NOT_FOUND_BY_MEMBER = "회원 ID에 해당하는 결제 내역을 찾을 수 없습니다."; // 25.03.12
    public static final String PAYMENT_HISTORY_NOT_FOUND_BY_IMPUID = "결제 고유 ID에 해당하는 결제 내역을 찾을 수 없습니다."; // 25.03.12
    public static final String PAYMENT_CANCEL_FAILURE = "결제 취소 실패 하였습니다."; // 25.03.12

    public static final String MEMBER_NOT_MATCH = "회원 정보가 일치하지 않습니다."; // 25.04.26
    public static final String CREDENTIALS_NOT_MATCH = "이메일 & 비밀번호가 일치하지 않습니다."; //25.06.13
    public static final String EMAIL_NOT_VERIFIED = "인증되지 않은 이메일 입니다."; // 25.06.14
}