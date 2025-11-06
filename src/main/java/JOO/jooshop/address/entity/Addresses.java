package JOO.jooshop.address.entity;

import JOO.jooshop.address.model.AddressesReqeustDto;
import JOO.jooshop.members.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Hibernate용 기본 생성자
@AllArgsConstructor
@Table(name = "addresses")
public class Addresses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "address_name")
    private String addressName;

    @Column(name = "recipient")
    private String recipient;

    @Column(name = "post_code", length = 100, nullable = false)
    private String postCode;

    @Column(name = "address")
    private String address;

    @Column(name = "detail_address")
    private String detailAddress;

    @Builder.Default
    @Column(name = "is_default_address")
    private boolean isDefaultAddress = false;

    @Column(name = "recipient_phone")
    private String recipientPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * DTO 기반 엔티티 생성 메서드
     */
    public static Addresses createAddress(AddressesReqeustDto dto, Member member) {
        LocalDateTime now = LocalDateTime.now();
        return Addresses.builder()
                .addressName(dto.getAddressName())
                .recipient(dto.getRecipient())
                .postCode(dto.getPostCode())
                .address(dto.getAddress())
                .detailAddress(dto.getDetailAddress())
                .isDefaultAddress(dto.isDefaultAddress())
                .recipientPhone(dto.getRecipientPhone())
                .member(member)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
