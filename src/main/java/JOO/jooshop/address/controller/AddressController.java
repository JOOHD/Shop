package JOO.jooshop.address.controller;

import JOO.jooshop.address.entity.Addresses;
import JOO.jooshop.address.model.AddressesRequstDto;
import JOO.jooshop.address.service.AddressService;
import JOO.jooshop.global.authorization.MemberAuthorizationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class AddressController {

    private final AddressService addressService;

    /**
     * 특정 회원의 주소 목록을 조회합니다.
     *
     * @param memberId 회원 식별자
     * @return 회원이 등록한 주소 리스트 반환
     */
    @GetMapping("/address/{memberId}")
    public ResponseEntity<List<Addresses>> fetchAddressList(@PathVariable("memberId") Long memberId) {
        // 회원 본인 여부 검증 (인증/인가)
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);

        return addressService.fetchAddressList(memberId);
    }

    /**
     * 특정 회원의 주소를 새로 추가합니다.
     *
     * @param memberId  회원 식별자
     * @param addressDto 등록할 주소 정보
     * @return 생성된 주소 정보 반환
     */
    @PostMapping("/address/{memberId}")
    public ResponseEntity<Addresses> createAddress(@PathVariable("memberId") Long memberId, @RequestBody AddressesRequstDto addressDto) {
        // 회원 본인 여부 검증
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);

        return addressService.createAddress(memberId, addressDto);
    }

    /**
     * 특정 회원의 상세 주소 정보를 조회합니다.
     *
     * @param memberId  회원 식별자
     * @param addressId 주소 식별자
     * @return 주소 상세 정보 반환
     */
    @GetMapping("/address/{memberId}/{addressId}")
    public ResponseEntity<?> fetchDetailAddress(@PathVariable("memberId") Long memberId, @PathVariable Long addressId) {
        // 회원 본인 여부 검증
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);

        return addressService.fetchDetailAddress(memberId, addressId);
    }

    /**
     * 특정 회원의 주소 정보를 수정합니다.
     *
     * @param memberId   회원 식별자
     * @param addressId  수정할 주소 식별자
     * @param addressDto 수정할 주소 정보
     * @return 수정 결과 반환
     */
    @PutMapping("/address/{memberId}/{addressId}")
    public ResponseEntity<?> updateDetailAddress(@PathVariable("memberId") Long memberId, @PathVariable Long addressId, @RequestBody AddressesRequstDto addressDto) {
        // 회원 본인 여부 검증
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);

        return addressService.updateDetailAddress(memberId, addressId, addressDto);
    }

    /**
     * 특정 회원의 주소 정보를 삭제합니다.
     *
     * @param memberId  회원 식별자
     * @param addressId 삭제할 주소 식별자
     * @return 삭제 결과 반환
     */
    @DeleteMapping("/address/{memberId}/{addressId}")
    public ResponseEntity<?> deleteDetailAddress(@PathVariable("memberId") Long memberId, @PathVariable Long addressId) {
        // 회원 본인 여부 검증
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);

        return addressService.deleteDetailAddress(memberId, addressId);
    }

    /**
     * 특정 회원의 기본 주소를 설정합니다.
     *
     * @param memberId  회원 식별자
     * @param addressId 기본 주소로 설정할 주소 식별자
     * @return 기본 주소 설정 결과 반환
     */
    @PutMapping("/address/default/{memberId}/{addressId}")
    public ResponseEntity<?> updateIsDefaultAddress(@PathVariable("memberId") Long memberId, @PathVariable Long addressId) {
        // 회원 본인 여부 검증
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);

        return addressService.setDefaultAddress(memberId, addressId);
    }
}
