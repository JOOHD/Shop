package JOO.jooshop.address.model;


import lombok.Data;

@Data
public class AddressesRequstDto {

    private String addressName;     // 주소지 (집, 회사)

    private String recipient;       // 수령인

    private String postCode;        // 우편번호

    private String address;         // 주소

    private String detailAddress;   // 상세 주소

    private boolean defaultAddress; // 기본 주소 여부

    private String recipientPhone;  // 수령인 전화번호

}
