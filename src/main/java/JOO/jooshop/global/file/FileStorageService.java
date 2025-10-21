package JOO.jooshop.global.file;

import jakarta.mail.Multipart;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    /*
        fileStorageService
        파일을 어디에 저장하고, URL을 어떻게 리턴하는가”를 처리하는 인프라스트럭처 레벨

        ext = extension (파일 확장자)
        relativePath = 파일 저장 경로 전체를 외부에서 미리 조합할 때 유용
        subPath = “thumbnails”, “contentImgs” 등 저장 위치만 구분할 때 직관적
     */

    private static final String BASE_DIR = "src/main/resources/static/upload";

    /* 파일 저장 */
    public String saveFileUrl(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) return null;

        String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        Path dirPath = Paths.get(BASE_DIR + subDir);
        Files.createDirectories(dirPath);

        Path filePath = dirPath.resolve(fileName);
        file.transferTo(filePath.toFile());

        return "/" + subDir + "/" + fileName; // DB에 저장될 URL
    }

    /* 파일 삭제 */
    public void deleteFile(String subPath) {
        if (subPath == null || subPath.isBlank()) return;

        try {
            Path path = Paths.get(BASE_DIR + subPath.replaceFirst("^/", ""));
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 실패: " + subPath, e);
        }
    }
}
    /*
    FileStorageService 클래스 구현으로 두 클래스 메서드가 제거 됨(요약)
    상세 이미지 저장
    private String saveContentImage(MultipartFile image, String uploadsDir, String dbBasePath) throws IOException {
        if (image == null || image.isEmpty()) return null;

        String originalFilename = image.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) return null;

        String fileName = UUID.randomUUID().toString().replace("-", "") + "_" + originalFilename;
        String filePath = uploadsDir + fileName;
        String dbFilePath = dbBasePath + fileName;

        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.write(path, image.getBytes());

        return dbFilePath;
    }

    썸네일 이미지 저장
    private String saveThumbnail(MultipartFile image, String uploadDir) throws IOException {
        String fileName = UUID.randomUUID().toString().replace("-", "") + "_" + image.getOriginalFilename();
        String filePath = uploadDir + fileName;
        String dbFilePath = "uploads/thumbnails/" + fileName;

        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.write(path, image.getBytes());

        return dbFilePath;
    }
     */

