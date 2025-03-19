package JOO.jooshop.address.repository;

import JOO.jooshop.address.entity.Addresses;
import JOO.jooshop.members.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Addresses, Long> {

    Optional<List<Addresses>> findAllByMember(Member member);

    Optional<Addresses> findByAddressId(Long addressId);

    List<Addresses> findAllByMemberId(Long memberId);
}