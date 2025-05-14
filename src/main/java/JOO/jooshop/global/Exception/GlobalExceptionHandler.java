package JOO.jooshop.global.Exception;

import JOO.jooshop.global.ResponseMessageConstants;
import JOO.jooshop.payment.entity.PaymentHistory;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import okhttp3.Response;
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

    /**
     * [GlobalExceptionHandler 사용 원칙]

     * (1) ex.getMessage() 를 사용하는 경우
          - 상황에 따라 에러 메시지가 달라질 수 있는 경우 (동적인 메시지)
          - ex) 잘못된 요청 파라미터, DB 조회 실패, 입력값 유효성 오류 등
          - 대표 핸들러 : IllegalArgumentException, NoSuchElementException, Validation 관련 예외

     * (2) ResponseMessageConstants 등 상수를 사용하는 경우
          - 항상 고정된 메시지를 클라이언트에 내려야 하는 경우 (정택적으로 통일)
          - ex) 회원 불일치, 결제 취소 실패, 결제 내역 없음 등
          - 대표 헨들러 : MemberNotMatchExcpetion, PaymentCancelFailureException,,,
     *
     * throws vs throw
     * throw : 메서드 내부, 메시지 포함해서 개발자(혹은 사용자)한테 이 에러가 어떤 문제인지 알리는 역할
     * throws : throws & try~catch
     */

    /* DB 에서 데이터를 찾지 못한 경우 */
    @ExceptionHandler(NoSuchElementException.class) // 상황별 데이터
    public ResponseEntity<String> handleNoSuchElementException(NoSuchElementException ex) {
        String errorMessage = ex.getMessage();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found DB Data: " + ex.getMessage());
    }

    /* 잘못된 요청 데이터(입력값 문제) 예외 */
    @ExceptionHandler(IllegalArgumentException.class) // 어떤 인자가 잘못됐는지
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        String errorMessage = ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
    }

    /* 유효성 검사(@Valid, DTO 전체) 예외 */
    @ExceptionHandler(MethodArgumentNotValidException.class) // @Valid DTO 단체 검증 실패 → 실패한 필드의 에러 메시지 가져옴.
    public ResponseEntity<String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = "유효성 검사 실패 : " + ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
    }

    /* 잘못된 요청 데이터(입력값 문제) 예외 */
    @ExceptionHandler(HttpMessageNotReadableException.class) // 본문을 읽을 수 없는 문제 (JSON 파싱 실패 등)
    public ResponseEntity<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("요청 본문을 읽을 수 없습니다 : " + ex.getMessage());
    }

    /* 잘못된 요청 데이터(입력값 문제) 예외 */
    @ExceptionHandler(NullPointerException.class) // 필수 필드 누락 등으로 발생
    public ResponseEntity<String> handleNullPointerException(NullPointerException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("필수 필드입니다 : " + ex.getMessage());
    }

    /* 보안 및 인증 관련 문제 */
    @ExceptionHandler(SecurityException.class) // 인증/인가 문제
    public ResponseEntity<String> handleSecurityException(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    /* 비즈니스 로직의 상태 오류 */
    @ExceptionHandler(IllegalStateException.class) // 상태 오류로 인한 문제,
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException ex) {
        String errorMessage = ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
    }

    /* 잘못된 요청 데이터(입력값 문제) 예외 */
    @ExceptionHandler(InvalidParameterException.class) // 입력값 문제
    public ResponseEntity<String> handleInvalidParameterException(InvalidParameterException ex) {
        String errorMessage = ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
    }

    //////////////////////////////// ResponseMessageConstants ////////////////////////////////

    /* 사용자 불일치 실패 */
    @ExceptionHandler(MemberNotMatchException.class) // 회원 불일치는 항상 고정된 메시지
    public ResponseEntity<String> handleMemberNotMatchException(MemberNotMatchException ex) {
        String errorMessage = ResponseMessageConstants.MEMBER_NOT_MATCH;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
    }

    /* 토큰 만료 */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<?> handleExpiredJwt(ExpiredJwtException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 만료되었습니다.");
    }

    /* RefreshToken not found */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh 토큰이 존재하지 않습니다.");
    }

    /* 비즈니스 로직의 상태 오류 */
    @ExceptionHandler(PaymentCancelFailureException.class) // 결제 취소 실패도 정책상 고정된 메시지
    public ResponseEntity<String> handlePaymentCancelFailureException(PaymentCancelFailureException ex) {
        String errorMessage = ResponseMessageConstants.PAYMENT_CANCEL_FAILURE;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
    }

    /* 결제 내역을 찾을 수 없는 경우 */
    @ExceptionHandler(PaymentHistoryNotFoundException.class) // 결제 내역 없음 → 항상 고정된 에러
    public ResponseEntity<String> handlePaymentHistoryNotFoundException(PaymentHistoryNotFoundException ex) {
        String errorMessage = ResponseMessageConstants.PAYMENT_HISTORY_NOT_FOUND;
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
    }

    /* 유효성 검사(@Validated @RequestParam, @PathVariable 단일) 예외 */
    @ExceptionHandler(ConstraintViolationException.class) // @RequestParam, @PathVariable 단일 파라미터 검증 실패
    public ResponseEntity<String> handleConstraintViolationException(ConstraintViolationException ex) {
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        StringBuilder errorMessage = new StringBuilder();
        for (ConstraintViolation<?> violation : violations) {
            errorMessage.append(violation.getMessage()).append("\n");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage.toString());
    }
}
