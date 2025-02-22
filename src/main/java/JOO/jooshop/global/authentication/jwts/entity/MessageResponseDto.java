package JOO.jooshop.global.authentication.jwts.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MessageResponseDto {

    private String code;

    private String message;
}

