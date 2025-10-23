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

    // 🔹 static 폴더 내 저장 기본 경로
    private static final String BASE_DIR = "src/main/resources/static/uploads/";

    /**
     *  파일 저장 (MultipartFile)
     * @param file 업로드할 파일
     * @param subDir 하위 디렉토리명 (예: "thumbnails", "contentImgs")
     * @return DB에 저장할 상대 URL (예: "/upload/thumbnails/abc123.jpg")
     */
    public String saveFile(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) return null;

        // 확장자 추출
        String ext = "";
        int idx = originalFilename.lastIndexOf(".");
        if (idx != -1) ext = originalFilename.substring(idx);

        // 랜덤 파일명 생성
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 디렉토리 생성
        Path dirPath = Paths.get(BASE_DIR + subDir);
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
            Path path = Paths.get(BASE_DIR + cleanPath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 실패: " + relativePath, e);
        }
    }
}
