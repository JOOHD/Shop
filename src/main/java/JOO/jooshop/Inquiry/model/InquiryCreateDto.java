package JOO.jooshop.Inquiry.model;

import JOO.jooshop.Inquiry.entity.Inquiry;
import JOO.jooshop.Inquiry.entity.enums.InquiryType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class InquiryCreateDto {

    private String name;
    private String email;
    private InquiryType inquiryType;
    @NotBlank(message = "제목은 필수입니다.")
    private String inquiryTitle;
    @NotBlank(message = "내용은 필수입니다.")
    private String inquiryContent;
    private String password;

    public InquiryCreateDto(Inquiry inquiry) {
        this(
                inquiry.getName(),
                inquiry.getEmail(),
                inquiry.getInquiryType(),
                inquiry.getInquiryTitle(),
                inquiry.getInquiryContent(),
                inquiry.getPassword()
        );

    }

}
