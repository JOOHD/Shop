package JOO.jooshop.Inquiry.repository;

import JOO.jooshop.Inquiry.entity.Inquiry;
import JOO.jooshop.Inquiry.entity.enums.InquiryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findByProductProductId(Long productId);

    @Modifying
    @Query("UPDATE Inquiry i SET i.inquiryType = :inquiryType, i.inquiryTitle = :inquiryTitle WHERE i.inquiryId = :inquiryId")
    void updateInquiryFields(@Param("inquiryId") Long inquiryId, @Param("inquiryType") InquiryType inquiryType, @Param("inquiryTitle") String inquiryTitle);
}
