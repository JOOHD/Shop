package JOO.jooshop.profiile.service;

import JOO.jooshop.global.image.ImageUtil;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepository;
import JOO.jooshop.profiile.entity.Profiles;
import JOO.jooshop.profiile.entity.enums.MemberAges;
import JOO.jooshop.profiile.entity.enums.MemberGender;
import JOO.jooshop.profiile.model.MemberDTO;
import JOO.jooshop.profiile.model.MemberProfileDTO;
import JOO.jooshop.profiile.model.ProfileUpdateDTO;
import JOO.jooshop.profiile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final MemberRepository memberRepository;
    private final ProfileRepository profileRepository;

    public MemberProfileDTO getProfile(Long memberId) {
        Profiles profile = profileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new NoSuchElementException("Profile not found: " + memberId));

        MemberDTO memberDTO = MemberDTO.createMemberDto(profile.getMember());
        return MemberProfileDTO.createMemberProfileDto(profile, memberDTO);
    }

    @Transactional
    public void updateProfile(Long memberId, ProfileUpdateDTO dto) {
        fillJoinedAtInNewTransaction();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("Member not found: " + memberId));

        Profiles profile = profileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new NoSuchElementException("Profile not found: " + memberId));

        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            member.changeNickname(dto.getNickname());
        }

        if (dto.getAge() != null && !dto.getAge().isBlank()) {
            profile.changeMemberAge(MemberAges.valueOf(dto.getAge()));
        }

        if (dto.getGender() != null && !dto.getGender().isBlank()) {
            profile.changeMemberGender(MemberGender.valueOf(dto.getGender()));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fillJoinedAtInNewTransaction() {
        int updated = memberRepository.fillNullJoinedAt();
        log.info("Updated {} members' joinedAt", updated);
    }

    @Cacheable(value = "profileImages", key = "#memberId")
    public String getProfileImage(Long memberId) throws Exception {
        Profiles profile = profileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new NoSuchElementException("Profile not found: " + memberId));
        return profile.getProfileImgPath();
    }

    @Transactional
    @CachePut(value = "profileImages", key = "#memberId")
    public ResponseEntity<String> uploadProfileImage(Long memberId, MultipartFile imageFile) {
        String uploadsDir = "src/main/resources/static/uploads/profileImgs/";

        String fileName = UUID.randomUUID().toString().replace("-", "") + imageFile.getOriginalFilename();
        String filePath = uploadsDir + fileName;
//        String dbFilePath = "/uploads/profileImgs/" + fileName;

        try {
            String resizedFileName = ImageUtil.resizeImageFile(imageFile, filePath, "jpeg");
            String resizedDbFilePath = "/uploads/profileImgs/" + resizedFileName;

            Profiles profile = profileRepository.findByMemberId(memberId)
                    .orElseThrow(() -> new NoSuchElementException("Profile not found: " + memberId));

            profile.changeProfileImage(resizedDbFilePath);
            return ResponseEntity.ok(profile.getProfileImgPath());

        } catch (IOException e) {
            log.error("Error while processing the image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("이미지 처리 중 오류가 발생했습니다.");
        }
    }

    @Transactional
    public void deleteProfileImage(Long memberId) {
        Profiles profile = profileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new NoSuchElementException("Profile not found"));

        String currentImagePath = profile.getProfileImgPath();

        profile.changeProfileImage(null);

        if (currentImagePath != null && !currentImagePath.isBlank()) {
            String fullPath = "src/main/resources/static" + currentImagePath;
            deleteImageFile(fullPath);
        }
    }

    public static void deleteImageFile(String imagePath) {
        try {
            Path path = Paths.get(imagePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("파일 삭제 중 오류", e);
        }
    }
}