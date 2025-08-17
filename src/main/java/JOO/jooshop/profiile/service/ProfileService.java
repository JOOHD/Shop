package JOO.jooshop.profiile.service;

import JOO.jooshop.global.authorization.MemberAuthorizationUtil;
import JOO.jooshop.global.authorization.RequiresRole;
import JOO.jooshop.global.image.ImageUtil;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.profiile.entity.Profiles;
import JOO.jooshop.profiile.entity.enums.MemberAges;
import JOO.jooshop.profiile.entity.enums.MemberGender;
import JOO.jooshop.profiile.model.ProfileUpdateDTO;
import JOO.jooshop.profiile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true) // 조회(readOnly), 저장/수정/삭제
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    /*
        - 이미지 경로를 DB에 직접 저장하는 방식 사용
        - 로컬 파일 저장 방식을 사용, 서버 배포 환경에 따라는 S3 같은 외부 스토리지 연동
        - 캐싱 활숑하여 프로필 이미지 빈번한 조회 성능 최적화
     */

    private final MemberRepositoryV1 memberRepository;
    private final ProfileRepository profileRepository;

    /* 프로필 생성(회원가입 시, INSERT) */
    @Transactional
    @RequiresRole({ MemberRole.USER, MemberRole.SELLER})
    public void saveProfileBySignup(Profiles memberProfiles) {
        profileRepository.save(memberProfiles);
    }

    /* 프로필 수정 */
    @Transactional
    public void updateProfile(Long memberId, ProfileUpdateDTO dto) {
        // 1. Member entity 수정 (닉네임)
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("Member not found: " + memberId));

        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            member.setNickname(dto.getNickname());
            memberRepository.save(member);
        }

        // 2. Profiles 엔티티 수정 (나이, 성별)
        Profiles profile = profileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new NoSuchElementException("Profile not found: " + memberId));

        if (dto.getAge() != null && !dto.getAge().isBlank()) {
            profile.updateMemberAge(MemberAges.valueOf(dto.getAge()));
        }

        if (dto.getGender() != null && !dto.getGender().isBlank()) {
            profile.updateMemberGender(MemberGender.valueOf(dto.getGender()));
        }

        profileRepository.save(profile);

        // 3. joinedAt null 처리 (Member 테이블 전체)
        int updatedCount = memberRepository.fillNullJoinedAt();
        log.info("Updated " + updatedCount + " members' joinedAt");
    }

    /* 프로필 이미지 조회 (SELECT) */
    @Cacheable(value = "profileImages", key = "#memberId") // 캐시에 존재하면 DB 조회 없이 바로 반환, 없으면 DB 조회 후, 캐시에 저장
    @RequiresRole({ MemberRole.USER, MemberRole.SELLER})
    public String getProfileImage(Long memberId) throws Exception {
        Optional<Profiles> memberProfileOpt = profileRepository.findByMemberId(memberId);
        if (memberProfileOpt.isPresent()) {
            Profiles memberProfile = memberProfileOpt.get();
            return memberProfile.getProfileImgPath();
        } else {
            throw new Exception("Profile not found for member Id : " + memberId);
        }
    }

    /* 프로필 이미지 업로드 및 수정 (UPDATE) */
    @Transactional
    @CachePut(value = "profileImages", key = "#memberId")
    @RequiresRole({ MemberRole.USER, MemberRole.SELLER})
    public ResponseEntity<String> uploadProfileImageV3(@PathVariable("memberId") Long memberId, @RequestPart MultipartFile imageFile) {
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);
        String uploadsDir = "/src/main/resources/static/uploads/profileimg/";

        String fileName = UUID.randomUUID().toString().replace("-", "") + imageFile.getOriginalFilename();
        String filePath = uploadsDir + fileName;
        String dbFilePath = "/uploads/profileimg/" + fileName;

        log.info("Original file size: " + imageFile.getSize() + " bytes");

        try {
            long start = System.currentTimeMillis();
            String resizedFileName = ImageUtil.resizeImageFile(imageFile, filePath, "jpeg");

            String resizedFilePath = uploadsDir + resizedFileName;
            Optional<Profiles> memberProfileOpt = profileRepository.findByMemberId(memberId);
            if (memberProfileOpt.isPresent()) {
                Profiles memberProfile = memberProfileOpt.get();
                memberProfile.setProfileImgName(resizedFileName);
                memberProfile.setProfileImgPath(dbFilePath);
                profileRepository.save(memberProfile);
                long end = System.currentTimeMillis();
                log.info("Time taken to save the image locally: " + (end - start) + " milliseconds");
                return ResponseEntity.ok().body(memberProfile.getProfileImgPath());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile not found for member Id : " + memberId);
            }
        } catch (IOException e) {
            log.error("Error while processing the image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while processing the image");
        }
    }

    /* 프로필 이미지 삭제 (DELETE) */
    @Transactional
    @RequiresRole({ MemberRole.USER, MemberRole.SELLER})
    public void deleteProfileImage(Long memberId) {
        Optional<Profiles> memberProfileOpt = profileRepository.findByMemberId(memberId);
        if (memberProfileOpt.isPresent()) {
            Profiles memberProfile = memberProfileOpt.get();
            String imagePath = "src/main/resources/static" + memberProfile.getProfileImgName();
            memberProfile.setProfileImage(null);
            profileRepository.save(memberProfile);
            deleteImageFile(imagePath);
        } else {
            throw new NoSuchElementException("Profile not found");
        }
    }

    /* 파일 삭제 유틸 */
    public static void deleteImageFile(String imagePath) {
        try {
            Path path = Paths.get(imagePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Exception handling in case of file deletion error
            e.printStackTrace();
        }
    }
}


















