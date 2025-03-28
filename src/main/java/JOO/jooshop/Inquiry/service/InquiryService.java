package JOO.jooshop.Inquiry.service;

import JOO.jooshop.Inquiry.entity.Inquiry;
import JOO.jooshop.Inquiry.model.InquiryCreateDto;
import JOO.jooshop.Inquiry.model.InquiryDto;
import JOO.jooshop.Inquiry.model.InquiryUpdateDto;
import JOO.jooshop.Inquiry.repository.InquiryRepository;
import JOO.jooshop.global.ResponseMessageConstants;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authorization.MemberAuthorizationUtil;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static JOO.jooshop.global.ResponseMessageConstants.*;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class InquiryService {
    public final InquiryRepository inquiryRepository;
    public final MemberRepositoryV1 memberRepository;
    public final ProductRepositoryV1 productRepository;
    private final JWTUtil jwtUtil;
    public final ModelMapper modelMapper;

    /**
     * 문의 작성
     * @param requestDto 문의 엔티티 필드
     * @param productId 문의 작성할 상품
     * @param request
     * @return
     */
    @Transactional
    public Long createInquiry(InquiryCreateDto requestDto, Long productId, HttpServletRequest request) {
        String authHeader = request.getHeader("Authentication");

        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException(ResponseMessageConstants.PRODUCT_NOT_FOUND));

        Inquiry inquiry = Inquiry.builder()
                .product(product)
                .inquiryType(requestDto.getInquiryType())
                .inquiryTitle(requestDto.getInquiryTitle())
                .inquiryContent(requestDto.getInquiryContent())
                .password(requestDto.getPassword())
                .build();

        // header 호출 이유는, 사용자가 로그인한 상태인지 확인하기 위해
        // header = token 인증 정보로 로그인 여부 판단
        if (authHeader != null) {
            // 로그인한 사용자 → Member 정보 가져옴
            Long memberId = MemberAuthorizationUtil.getLoginMemberId();
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new NoSuchElementException(ResponseMessageConstants.MEMBER_NOT_FOUND));

            inquiry.createInquiryWriter(member, member.getNickname(), member.getEmail());
            // 비회원 → 요청에서 이름과 이메일을 직접 가져옴
        } else {
            inquiry.createInquiryWriter(null,requestDto.getName(), requestDto.getEmail());
        }

        inquiryRepository.save(inquiry);
        return inquiry.getInquiryId();
    }

    /**
     * 전체 문의 리스트
     * @return
     */
    public List<InquiryDto> allInquiryList() {
        List<InquiryDto> inquiryDtoList = new ArrayList<>();
        List<Inquiry> inquiryList = inquiryRepository.findAll();

        for (Inquiry inquiry : inquiryList) {
            inquiryDtoList.add(InquiryDto.mapInquiryToDto(inquiry, false ));
        }

        return inquiryDtoList;
    }

    /**
     * 특정 상품의 문의글 리스트
     * @param productId
     * @return
     */
    public List<InquiryDto> inquiryListByProductId(Long productId) {
        return inquiryRepository.findByProduct_ProductId(productId).stream()
                .map(inquiry -> InquiryDto.mapInquiryToDto(inquiry, false))
                .collect(Collectors.toList());
    }

    /**
     * 문의글 상세보기
     * @param inquiryId
     * @return
     */
    public InquiryDto inquiryDetail(Long inquiryId) {
        Inquiry inquiryDetail = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new NoSuchElementException(WRITING_NOT_FOUND));


        return InquiryDto.mapInquiryToDto(inquiryDetail, true);
    }

    /**
     * 문의글 수정
     * @param inquiryId
     * @param requestDto
     * @param password
     * @return
     */
    public Inquiry updateInquiry(Long inquiryId, InquiryUpdateDto requestDto, String password) {

        Inquiry existingInquiry = validatePasswordAndGetInquiry(inquiryId, password);

        inquiryRepository.updateInquiryFields(inquiryId, requestDto.getInquiryType(), requestDto.getInquiryContent());

        return inquiryRepository.save(existingInquiry);
    }

    /**
     * 문의글 삭제
     * @param inquiryId
     */
    public void deleteInquiry(Long inquiryId, String password) {

        Inquiry existingInquiry = validatePasswordAndGetInquiry(inquiryId, password);

        inquiryRepository.delete(existingInquiry);
    }

    /**
     * 수정/삭제시 비밀번호를 검증하고 기존 문의글 반환하는 함수
     * @param inquiryId
     * @param password
     * @return
     */
    private Inquiry validatePasswordAndGetInquiry(Long inquiryId, String password) {
        Inquiry existingInquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new NoSuchElementException(WRITING_NOT_FOUND));

        // 비밀번호 검증
        if (!existingInquiry.getPassword().equals(password)) {
            throw new IllegalArgumentException("잘못된 비밀번호 입니다.");
        }

        return existingInquiry;
    }
}











