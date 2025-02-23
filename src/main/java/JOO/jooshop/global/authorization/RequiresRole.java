package JOO.jooshop.global.authorization;

import JOO.jooshop.members.entity.enums.MemberRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 어노테이션 유지되는 기간, RUNTIME : 프로그램 실행 중, 어노테이션 정보를 읽을 수 잇다.
 AuthoroizationAspect(AOP)에서 @RequiresRole 을 읽기 위해, RUNTIME 필수
 Retention(RetentionPolicy.class) RUNTIME 이 없으면, AOP 적용 불가
 
 ElementType.TYPE : @RequiresRole 클래스 위에 선언해도 적용 가능
 
 @interface : 어노테이션을 적용하는 문법, compiler 가 어노테이션으로 인식 
 */
@Retention(RetentionPolicy.RUNTIME) 
@Target({ElementType.METHOD, ElementType.TYPE}) 
public @interface RequiresRole {
    MemberRole[] value();
}
