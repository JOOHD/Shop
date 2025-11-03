package JOO.jooshop.global.file;

import JOO.jooshop.global.image.ImageUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 파일 저장 및 삭제 전담 서비스
 */
@Service
public class FileStorageService {

    // FileStorageService가 “경로 + 리사이징 여부 + 실제 저장”을 전부 담당

    private final Path BASE_UPLOAD_PATH  = Paths.get(System.getProperty("user.dir"), "uploads");

    /**
     * 파일 저장
     * @param file 업로드할 MultipartFile
     * @param subDir 하위 디렉토리명 (예: "thumbnails", "contentImgs")
     * @return DB에 저장할 상대 URL (예: "/upload/thumbnails/abc123.jpg")
     */
    public String saveFile(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String originalFilename = file.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        // 랜덤 파일명 생성
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 디렉토리 생성
        Path dirPath = BASE_UPLOAD_PATH.resolve(subDir);
        Files.createDirectories(dirPath);

        Path filePath = dirPath.resolve(fileName);
        file.transferTo(filePath.toFile());

        // DB  URL 저장
        return (subDir + "/" + fileName).replaceAll("//+", "/"); // ex: "thumbnails/abc.jpg"
    }

    /**
     * 파일 삭제
     * @param relativePath DB에 저장된 상대 경로
     */
    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;

        try {
            String cleanPath = relativePath.replaceFirst("^/uploads/", "");
            Path fullPath = BASE_UPLOAD_PATH.resolve(cleanPath);
            Files.deleteIfExists(fullPath);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 실패: " + relativePath, e);
        }
    }

    /**
     * HTML/브라우저에서 접근 가능한 URL 생성
     * @param relativePath DB에 저장된 상대 경로
     * @return 절대 URL 형태 (ex: /uploads/thumbnails/xxx.jpg)
     */
    public String getUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return null;
        // 방어 코드 추가
        String cleaned = relativePath.replaceAll("^/+", "").replaceFirst("^upload/", "");
        return "/uploads/" + cleaned;
    }
}
