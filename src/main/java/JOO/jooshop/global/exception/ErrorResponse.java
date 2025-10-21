package JOO.jooshop.global.exception;

import java.time.LocalDateTime;

public class ErrorResponse {
    private int status;              // 상태 코드 ex) 400, 401, 500..
    private String error;            // 상태 이름 ex) BAD_REQEUST
    private String message;          // 에러 메시지

    private LocalDateTime timestamp; // 에러 발생 시간

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // Getter
    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
