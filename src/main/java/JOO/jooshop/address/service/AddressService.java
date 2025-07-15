package JOO.jooshop.address.service;

import JOO.jooshop.address.entity.Addresses;
import JOO.jooshop.address.model.AddressesReqeustDto;
import JOO.jooshop.address.repository.AddressRepository;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import static JOO.jooshop.global.Exception.ResponseMessageConstants.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final MemberRepositoryV1 memberRepository;

    /**
     * 회원의 특정 주소를 수정한다.
     * - 요청한 회원과 주소의 소유자가 일치하는지 검증
     * - 수정 후 기본 주소인 경우 기존 기본 주소를 모두 해제
     *
     * @param memberId  현재 요청한 회원 ID
     * @param addressId 수정할 주소 ID
     * @param addressDto 수정할 주소 정보 DTO
     * @return 수정 결과에 따른 ResponseEntity
     */
    public ResponseEntity<?> updateDetailAddress(Long memberId, Long addressId, AddressesReqeustDto addressDto) {

        Member member = findMember(memberId);
        Addresses addresses = Addresses.createAddress(addressDto, member);

        addressRepository.save(addresses);

        if (addresses.isDefaultAddress()) {
            resetDefaultAddress(memberId, addresses.getAddressId());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(addresses);
    }

    /**
     * 회원의 새로운 주소를 생성한다.
     * - 기본 주소로 등록할 경우 기존 기본 주소들을 모두 해제
     *
     * @param memberId   주소를 추가할 회원 ID
     * @param addressDto 새로 추가할 주소 정보 DTO
     * @return 생성된 주소 정보와 함께 ResponseEntity 반환
     */
    public ResponseEntity<Addresses> createAddress(Long memberId, AddressesReqeustDto addressDto) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(MEMBER_NOT_FOUND));
        Addresses addresses = Addresses.createAddress(addressDto, member);
        addressRepository.save(addresses);

        return ResponseEntity.status(HttpStatus.CREATED).body(addresses);
    }

    /**
     * 회원의 전체 주소 리스트를 조회한다.
     *
     * @param memberId 조회할 회원 ID
     * @return 회원이 등록한 모든 주소 리스트와 함께 ResponseEntity 반환
     */
    public ResponseEntity<List<Addresses>> fetchAddressList(Long memberId) {
        Member member = findMember(memberId);

        List<Addresses> memberAddressList = addressRepository.findAllByMember(member)
                .orElseThrow(() -> new NoSuchElementException(ADDRESS_NOT_FOUND));

        return ResponseEntity.status(HttpStatus.OK).body(memberAddressList);
    }

    /**
     * 회원의 특정 주소를 상세 조회한다.
     * - 요청한 회원과 주소의 소유자가 일치하는지 검증
     *
     * @param memberId  조회 요청한 회원 ID
     * @param addressId 조회할 주소 ID
     * @return 조회된 주소 정보와 함께 ResponseEntity 반환
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> fetchDetailAddress(Long memberId, Long addressId) {
        Addresses address = findAddress(addressId);

        validateAddressOwner(memberId, address);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ACCESS_DENIED);
    }

    /**
     * 회원의 특정 주소를 삭제한다.
     * - 요청한 회원과 주소의 소유자가 일치하는지 검증
     *
     * @param memberId  삭제 요청한 회원 ID
     * @param addressId 삭제할 주소 ID
     * @return 삭제 결과 메시지와 함께 ResponseEntity 반환
     */
    public ResponseEntity<?> deleteDetailAddress(Long memberId, Long addressId) {
        Addresses address = findAddress(addressId);
        validateAddressOwner(memberId, address);

        addressRepository.delete(address);
        return ResponseEntity.status(HttpStatus.OK).body(ADDRESS_DELETE_SUCCESS);
    }

    /**
     * 회원의 특정 주소를 기본 주소로 설정한다.
     * - 요청한 회원과 주소의 소유자가 일치하는지 검증
     * - 기본 주소로 설정 시 기존 기본 주소를 모두 해제
     *
     * @param memberId  기본 주소 설정 요청한 회원 ID
     * @param addressId 기본 주소로 설정할 주소 ID
     * @return 기본 주소 설정 결과와 함께 ResponseEntity 반환
     */
    public ResponseEntity<?> setDefaultAddress(Long memberId, Long addressId) {
        Addresses address = findAddress(addressId);
        validateAddressOwner(memberId, address);

        resetDefaultAddress(memberId, address.getAddressId());
        address.setDefaultAddress(true);
        addressRepository.save(address);

        return ResponseEntity.status(HttpStatus.OK).body(address);
    }

    /** =================== 공통 메서드 =================== */

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(MEMBER_NOT_FOUND));
    }

    private Addresses findAddress(Long addressId) {
        return addressRepository.findByAddressId(addressId)
                .orElseThrow(() -> new NoSuchElementException(ADDRESS_NOT_FOUND));
    }

    private void validateAddressOwner(Long memberId, Addresses address) {
        if (!address.getMember().getId().equals(memberId)) {
            throw new SecurityException(ACCESS_DENIED);
        }
    }

    // 메소드가 호출되면, JPQL 이 기존 주소가 있더라도 한 번에 UPDATE 쿼리가 날아가고 끝!
    private void resetDefaultAddress(Long memberId, Long excludeAddressId) {
        addressRepository.resetDefaultAddresses(memberId, excludeAddressId);
    }

//    private void resetDefaultAddress(Long memberId, Long excludeAddressId) {
//        List<Addresses> allAddresses = addressRepository.findAllByMemberId(memberId);
//        for (Addresses addr : allAddresses) {
//            if (!addr.getAddressId().equals(excludeAddressId) && addr.isDefaultAddress()) {
//                addr.setDefaultAddress(false);
//                addressRepository.save(addr);
//            }
//        }
//    }

}

