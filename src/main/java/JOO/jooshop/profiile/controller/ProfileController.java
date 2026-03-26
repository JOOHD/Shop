package JOO.jooshop.profiile.controller;

import JOO.jooshop.global.authorization.MemberAuthorizationUtil;
import JOO.jooshop.profiile.model.MemberProfileDTO;
import JOO.jooshop.profiile.model.ProfileUpdateDTO;
import JOO.jooshop.profiile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{memberId}")
    public ResponseEntity<MemberProfileDTO> getProfile(@PathVariable Long memberId) {
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);
        return ResponseEntity.ok(profileService.getProfile(memberId));
    }

    @PutMapping("/{memberId}")
    public ResponseEntity<String> updateProfile(
            @PathVariable Long memberId,
            @RequestBody ProfileUpdateDTO dto
    ) {
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);
        profileService.updateProfile(memberId, dto);
        return ResponseEntity.ok("프로필이 수정되었습니다.");
    }

    @GetMapping("/image/{memberId}")
    public ResponseEntity<String> getProfileImage(@PathVariable Long memberId) throws Exception {
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);
        return ResponseEntity.ok(profileService.getProfileImage(memberId));
    }

    @PostMapping(value = "/image/{memberId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadProfileImage(
            @PathVariable Long memberId,
            @RequestParam("imageFile") MultipartFile imageFile
    ) {
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);
        return profileService.uploadProfileImage(memberId, imageFile);
    }

    @DeleteMapping("/image/{memberId}")
    public ResponseEntity<String> deleteProfileImage(@PathVariable Long memberId) {
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);
        profileService.deleteProfileImage(memberId);
        return ResponseEntity.ok("프로필 이미지가 삭제되었습니다.");
    }
}