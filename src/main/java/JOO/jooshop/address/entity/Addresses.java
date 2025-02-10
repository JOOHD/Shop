package JOO.jooshop.address.entity;


import JOO.jooshop.address.model.AddressesRequstDto;
import JOO.jooshop.members.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
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

    @Column(name = "is_default_address")
    private boolean isDefaultAddress = false;

    @Column(name = "recipient_phone")
    private String recipientPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Addresses() {
    }

    public Addresses(String addressName, String recipient, String postCode, String address, String detailAddress, boolean isDefaultAddress, String recipientPhone, Member member, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.addressName = addressName;
        this.recipient = recipient;
        this.postCode = postCode;
        this.address = address;
        this.detailAddress = detailAddress;
        this.isDefaultAddress = isDefaultAddress;
        this.recipientPhone = recipientPhone;
        this.member = member;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void editupdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }

    // Social Member 생성
    public static Addresses createAddress(AddressesRequstDto addressesRequstDto, Member member) {
        return new Addresses(addressesRequstDto.getAddressName(), addressesRequstDto.getRecipient(), addressesRequstDto.getPostCode(), addressesRequstDto.getAddress(), addressesRequstDto.getDetailAddress(), addressesRequstDto.isDefaultAddress(), addressesRequstDto.getRecipientPhone(), member, LocalDateTime.now(), LocalDateTime.now());
    }

    public Addresses updateAddress(AddressesRequstDto addressDto) {
        updateFieldsFromDto(addressDto);
        editupdatedAt();
        return this;
    }

    private void updateFieldsFromDto(AddressesRequstDto addressDto) {
        this.addressName = addressDto.getAddressName();
        this.recipient = addressDto.getRecipient();
        this.postCode = addressDto.getPostCode();
        this.address = addressDto.getAddress();
        this.detailAddress = addressDto.getDetailAddress();
        this.isDefaultAddress = addressDto.isDefaultAddress();
        this.recipientPhone = addressDto.getRecipientPhone();
    }

    public void setDefaultAddress(boolean isDefaultAddress) {
        this.isDefaultAddress = isDefaultAddress;
    }
}
