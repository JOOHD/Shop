package JOO.jooshop.address.service;

import JOO.jooshop.address.entity.Addresses;
import JOO.jooshop.address.model.AddressesRequstDto;
import JOO.jooshop.address.repository.AddressRepository;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import static JOO.jooshop.global.ResponseMessageConstants.ADDRESS_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final MemberRepositoryV1 memberRepository;

    public ResponseEntity<?> updateDetailAddress(Long memberId, Long addressId, AddressesRequstDto addressDto) {
        Addresses address = addressRepository.findByAddressId(addressId)
                .orElseThrow(() -> new NoSuchElementException(ADDRESS_NOT_FOUND));

        // DB.getMember().getId() = Client.request().getMemberId()
        if (address.getMember().getId().equals(memberId)) {
            Addresses updatedAddress = address.updateAddress(addressDto);

            if (updatedAddress.isDefaultAddress()) {
                List<Addresses> allAddresses = addressRepository.findAllByMemberId(memberId);
            }
        }
    }
}

