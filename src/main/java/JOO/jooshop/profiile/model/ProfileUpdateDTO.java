package JOO.jooshop.profiile.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateDTO {
    private String nickname;
    private String age;      // 문자열로 받으면 Enum 변환
    private String gender;   // 문자열로 받으면 Enum 변환
}
