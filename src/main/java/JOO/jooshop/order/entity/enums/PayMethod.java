package JOO.jooshop.order.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PayMethod {
    card,trans, vbank, phone;
    // 신용카드, 실시간 계좌이체, 가상계좌, 휴대폰 결제

    @JsonCreator
    public static PayMethod from(String s) {
        return PayMethod.valueOf(s.toLowerCase()); // 대소문자 구분x
    }
}
