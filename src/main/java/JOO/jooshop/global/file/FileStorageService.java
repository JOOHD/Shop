package JOO.jooshop.global.file;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 파일 저장 및 삭제를 담당하는 인프라 레벨 서비스.
 *
 * - MultipartFile 파일 저장
 * - 파일 삭제
 * - DB에는 실제 파일 경로 대신 접근 가능한 상대 URL("/upload/...") 형태로 저장
 */
@Service
public class FileStorageService {

    // static 폴더 내 저장 기본 경로
    // private static final String BASE_DIR = "src/main/resources/static/uploads/";

    // "src" 바깥 경로 — OS 기준 실제 경로, (C:/myproject/uploads/thumbnails)
    Path BASE_UPLOAD_PATH =
            Paths.get(System.getProperty("user.dir"), "uploads");

    /**
     * 파일 저장
     * @param file 업로드할 MultipartFile
     * @param subDir 하위 디렉토리명 (예: "thumbnails", "contentImgs")
     * @return DB에 저장할 상대 URL (예: "/upload/thumbnails/abc123.jpg")
     */
    public String saveFile(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) return null;

        // 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 랜덤 파일명 생성
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 디렉토리 생성
        Path dirPath = BASE_UPLOAD_PATH.resolve(subDir);
        Files.createDirectories(dirPath);

        // 파일 실제 저장
        Path filePath = dirPath.resolve(fileName);
        file.transferTo(filePath.toFile());

        // DB에는 "/upload/..." 형태의 상대 URL 저장
        return "/upload/" + subDir + "/" + fileName;
    }

    /**
     *  파일 삭제
     * @param relativePath DB에 저장된 상대 경로 ("/upload/thumbnails/xxx.jpg")
     */
    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;

        try {
            // "/upload/" 부분을 제거하고 실제 파일 경로로 변환
            String cleanPath = relativePath.replaceFirst("^/uploads/", "");
            Path fullPath = BASE_UPLOAD_PATH.resolve(cleanPath);
            Files.deleteIfExists(fullPath);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 실패: " + relativePath, e);
        }
    }
}
