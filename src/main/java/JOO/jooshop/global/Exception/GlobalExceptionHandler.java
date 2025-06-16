package JOO.jooshop.global.Exception;

import JOO.jooshop.global.ResponseMessageConstants;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.security.InvalidParameterException;
import java.util.NoSuchElementException;
import java.util.Set;

@ControllerAdvice
public class GlobalExceptionHandler {

    // ===================== 400 Bad Request =====================
    // 잘못된 요청 파라미터, 상태 오류 등 클라이언트 책임

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = "유효성 검사 실패 : " + ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return buildResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다 : " + ex.getMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(NullPointerException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "필수 필드입니다 : " + ex.getMessage());
    }

    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParameterException(InvalidParameterException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MemberNotMatchException.class)
    public ResponseEntity<ErrorResponse> handleMemberNotMatchException(MemberNotMatchException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ResponseMessageConstants.MEMBER_NOT_MATCH);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handlerInvalidCredentialsException(InvalidCredentialsException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ResponseMessageConstants.CREDENTIALS_NOT_MATCH);
    }

    @ExceptionHandler(PaymentCancelFailureException.class)
    public ResponseEntity<ErrorResponse> handlePaymentCancelFailureException(PaymentCancelFailureException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ResponseMessageConstants.PAYMENT_CANCEL_FAILURE);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        StringBuilder errorMessage = new StringBuilder();
        for (ConstraintViolation<?> violation : violations) {
            errorMessage.append(violation.getMessage()).append("\n");
        }
        return buildResponse(HttpStatus.BAD_REQUEST, errorMessage.toString());
    }

    // ===================== 401 Unauthorized =====================
    // 인증되지 않은 사용자 (토큰 없음/만료 등)

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(ExpiredJwtException e) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException e) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Refresh 토큰이 존재하지 않습니다.");
    }

    // ===================== 🔐 403 Forbidden =====================
    // 인증은 되었으나 권한이 부족하거나 제한된 상태

    @ExceptionHandler(UnverifiedEmailException.class)
    public ResponseEntity<ErrorResponse> handleUnverifiedEmail(UnverifiedEmailException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ResponseMessageConstants.EMAIL_NOT_VERIFIED);
    }

    // ===================== 기타 보안 예외 =====================

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // ===================== 404 Not Found =====================

    @ExceptionHandler(PaymentHistoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentHistoryNotFoundException(PaymentHistoryNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ResponseMessageConstants.PAYMENT_HISTORY_NOT_FOUND);
    }

    // ===================== 공통 응답 빌더 =====================

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), message));
    }
}
