package JOO.jooshop.profiile.controller;

import JOO.jooshop.global.authorization.MemberAuthorizationUtil;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.profiile.entity.Profiles;
import JOO.jooshop.profiile.entity.enums.MemberAges;
import JOO.jooshop.profiile.entity.enums.MemberGender;
import JOO.jooshop.profiile.model.MemberDTO;
import JOO.jooshop.profiile.model.MemberProfileDTO;
import JOO.jooshop.profiile.repository.ProfileRepository;
import JOO.jooshop.profiile.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileControllerV1 {

    /*
       공통 처리 과정
        - 본인 확인
          - MemberAuthorizationUtil.verifyUserIdMatch(memberId)
        - 프로필 조회 → 없으면 404
          - newNickname.replace("\"", "") → JSON 문자열로 넘어온 큰따옴표 제거
          - MemberAges/Gender.valueOf(newAge) → Enum 변환
        - 나이 업데이트 → 저장
        - 성공 → 200 OK
        - 실패 → 404 NOT FOUND or 500 SERVER ERROR
     */

    private final ProfileService profileService;
    private final ProfileRepository profileRepository;
    private final ObjectMapper objectMapper;
    private final MemberRepositoryV1 memberRepositoryV1;

    /* 프로필 조회 */
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberProfileDTO> getProfile(@PathVariable Long memberId) {
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);
        Optional<Profiles> memberProfiles = getMemberProfileByMemberId(memberId);

        if (memberProfiles.isPresent()) {
            Profiles profiles = memberProfiles.get();
            MemberDTO memberDTO = MemberDTO.createMemberDto(profiles.getMember()); // DTO 변환
            MemberProfileDTO memberProfileDTO = MemberProfileDTO.createMemberProfileDto(profiles, memberDTO);

            return ResponseEntity.ok(memberProfileDTO); // 200 (OK)
        }
        return ResponseEntity.notFound().build();       // 404 (NOT FOUND)
    }

    public Optional<Profiles> getMemberProfileByMemberId(Long memberId) {
        Optional<Profiles> memberProfileOpt = profileRepository.findByMemberId(memberId);
        return memberProfileOpt;
    }

    /* 프로필 이미지 경로 조회 */
    @GetMapping("/image/{memberId}")
    public ResponseEntity<?> getProfileImage(@PathVariable Long memberId) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "image/jpeg");
        return ResponseEntity.ok().body(profileService.getProfileImage(memberId)); // 500 (서비스 클래스 처리)
    }

    /* 프로필 이미지 업로드 */
    @PostMapping("/image/{memberId}")
    public ResponseEntity<String> postProfileImage(@PathVariable Long memberId, @RequestParam("imageFile") MultipartFile imageFile) throws Exception {
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);
        return profileService.uploadProfileImageV3(memberId, imageFile);
    }

    /* 프로필 이미지 삭제 */
    @DeleteMapping("/image/{memberId}")
    public ResponseEntity<String> deleteProfileImage(@PathVariable Long memberId) {
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);
        try {
            profileService.deleteProfileImage(memberId);
            return ResponseEntity.ok().body("Profile image deleted successfully for member id : " + memberId);
        } catch (NoSuchElementException e) { // 404(profile == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch(Exception e) { // 500 서비스 클래스 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deleting the image");
        }
    }

    /**
     * 1. request.user.id 가 요구하는 프로필 정보의 id 과 같은지 체크
     * 2. 회원의 nickname 을 변경하고 저장
     * @param memberId
     * @param newNickname
     * @return
     */

    /* 회원 닉네임 변경 */
    @PutMapping("/{memberId}/nickname")
    public ResponseEntity<String> updateMemberNickname(@PathVariable Long memberId, @RequestBody String newNickname) {
        // RequestBody 로 건너온 Nickname 을 enum 타입으로 변경
        newNickname = newNickname.replace("\"", "");
        log.info(newNickname);
        // request.user.id 가 요구하는 프로필 정보의 id 와 같은지 체크
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);
        // memberId 를 통해 member 조회
        Optional<Member> optionalMember = memberRepositoryV1.findById(memberId);
        if (optionalMember.isPresent()) {
            Member existingMember = optionalMember.get();
            existingMember.updateNickname(newNickname);
            memberRepositoryV1.save(existingMember);
            return ResponseEntity.ok("Member Nickname updated successfully.");
        }
        else{
            return ResponseEntity.notFound().build(); // 404 (NOT FOUND)
        }
    }

    /* 회원 나이 변경 */
    @PutMapping("/{memberId}/age")
    public ResponseEntity<String> updateMemberAge(@PathVariable Long memberId, @RequestBody String newAge) {
        // RequestBody 로 건너온 newAge 이 어떻게 넘어오는지 체크
        newAge = newAge.replace("\"", "");
        log.info(newAge);
        // request.user.id 가 요구하는 프로필 정보의 id 과 같은지 체크
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);
        // memberId 를 통해 member 조회
        Optional<Profiles> memberProfileOpt = profileRepository.findByMemberId(memberId);
        MemberAges newMemberAges = MemberAges.valueOf(newAge); // -> Enum 변환
        if (memberProfileOpt.isPresent()) {
            Profiles profiles = memberProfileOpt.get();
            profiles.updateMemberAge(newMemberAges);
            profileRepository.save(profiles);
            return ResponseEntity.ok("Member Age updated successfully.");
        }
        else{
            return ResponseEntity.notFound().build();
        }
    }

    /* 회원 성별 변경 */
    @PutMapping("/{memberId}/gender")
    public ResponseEntity<String> updateMemberGender(@PathVariable Long memberId, @RequestBody String newGender) {
        // RequestBody 로 건너온 newGender 이 어떻게 넘어오는지 체크
        newGender = newGender.replace("\"", "");
        log.info(newGender);
        // request.user.id 가 요구하는 프로필 정보의 id 과 같은지 체크
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);
        // memberId 를 통해 member 조회
        Optional<Profiles> memberProfileOpt = profileRepository.findByMemberId(memberId);
        MemberGender newMemberGender = MemberGender.valueOf(newGender);
        if(memberProfileOpt.isPresent()) {
            Profiles profiles = memberProfileOpt.get();
            profiles.updateMemberGender(newMemberGender);
            profileRepository.save(profiles);
            return ResponseEntity.ok("Member Age updated successfully.");
        }
        else{
            return ResponseEntity.notFound().build();
        }
    }
}
















