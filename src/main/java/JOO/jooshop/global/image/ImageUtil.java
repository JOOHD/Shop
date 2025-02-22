package JOO.jooshop.global.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

@Slf4j
public class ImageUtil {

    /**
     * 이미지의 크기를 줄이고, 필요에 따라 압축합니다.
     *
     * @param file 이미지 파일
     * @param filePath 이미지가 저장될 경로
     * @param formatName 이미지 포맷 (jpg, png 등)
     * @return 생성된 이미지 파일의 이름
     * @throws IOException 파일 입출력 오류
     * 
     * BufferedImage, Graphics2D - 이미지 리사이징
     * ImageWriter, setCompressionQuality() - 압축
     */
    public static String resizeImageFile(MultipartFile file,String filePath, String formatName) throws IOException {
        long startTime = System.currentTimeMillis(); // 작업 시작 시간
        
        // 입력된 이미지 정보를 가져옴
        BufferedImage inputImage = ImageIO.read(file.getInputStream());
        long initialSize = file.getSize();
        log.info("Initial file size: " + initialSize + " bytes");
        int originWidth = inputImage.getWidth();
        int originHeight = inputImage.getHeight();
        int newWidth = 400; // 새로운 너비 정의
        
        File outputFile = new File(filePath); // 출력될 파일 경로
        
        // 원본 이미지가 새로 정의된 너비보다 크다면, 크기 조정
        if (originWidth > newWidth) {
            int newHeight = (originHeight * newWidth) / originWidth;
            Image resizeImage = inputImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            BufferedImage newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = newImage.createGraphics();
            g2d.drawImage(resizeImage, 0, 0, null);
            g2d.dispose();

            // 임시 파일 크기가 2MB를 초과했다면, 이미지 압축을 고려
            if (outputFile.length() > 2 * 1024 * 1024) {
                log.info("Initial file size is larger than 2MB compressing...");

                // JPEG 이미지일 경우 품질을 조절하여 기본적으로 50% 압축
                if (formatName.equalsIgnoreCase("jpeg") || formatName.equalsIgnoreCase("jpg")) {
                    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
                    if (writers.hasNext()) {
                        ImageWriter writer = writers.next();
                        ImageWriteParam param = writer.getDefaultWriteParam();
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(0.5f); // 초기 품질은 50%

                        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
                            writer.setOutput(ios);
                            writer.write(null, new IIOImage(newImage, null, null), param);

                            // 파일 크기를 확인하며 추가로 압축. 최종 파일 크기가 1MB 이하가 되도록 반복
                            float quality = 0.5f;
                            while (outputFile.length() > 1024 * 1024 && quality > 0.2f) {
                                quality -= 0.2f;
                                param.setCompressionQuality(quality);
                                writer.write(null, new IIOImage(newImage, null, null), param);
                            }
                        }
                        writer.dispose();
                    }
                } else {
                    ImageIO.write(newImage, formatName, outputFile);
                }
            }
        } else {
            // 원본 이미지가 새로운 너비보다 작다면, 원본 그대로 저장
            file.transferTo(outputFile);
        }

        long endTime = System.currentTimeMillis(); // 작업 종료 시간
        long duration = endTime - startTime; // 작업 시간 계산

        log.info("Final file size: " + outputFile.length() + " bytes");
        log.info("Final file name: " + outputFile.getName() );
        log.info("Total time taken: " + duration + " ms");
        return outputFile.getName();
    }
}
















