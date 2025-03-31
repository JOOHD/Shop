package JOO.jooshop.address.repository;

import JOO.jooshop.address.entity.Addresses;
import JOO.jooshop.members.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Addresses, Long> {

    Optional<List<Addresses>> findAllByMember(Member member);

    Optional<Addresses> findByAddressId(Long addressId);

    List<Addresses> findAllByMemberId(Long memberId);

    @Transactional
    @Modifying(clearAutomatically = true) // @Query 는 기본적으로 SELECT만 가능하기 때문에 UPDATE/DELETE 하려면 @Modifying 필요
    @Query("UPDATE Addresses a SET a.isDefaultAddress = false WHERE a.member.id = :memberId AND a.addressId != :excludeId")
    void resetDefaultAddresses(@Param("memberId") Long memberId, @Param("excludeId") Long excludeId);

    /*
        포인트 정리

        @Modifying 필수!
        → @Query 는 기본적으로 SELECT 만 가능하기 때문에 UPDATE/DELETE 하려면 @Modifying 필요

        clearAutomatically = true
        → 영속성 컨텍스트에 남아있는 기존 엔티티가 반영되지 않는 문제 방지
        → 1차 캐시 초기화로 DB와 동기화 맞춤 (권장 옵션)

        UPDATE addresses
        SET default_address = false
        WHERE member_id = ?
        AND address_id != ?

        기존 기본 주소들이 모두 false가 됨 (단, excludeId는 제외)
     */
}