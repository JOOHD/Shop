package JOO.jooshop.members.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity(name = "certification")
@Table(name = "CERTIFICATION")
public class CertificationEntity {

    @Id
    private Long userId;

    private String email;

    private String certificationNumber;
}
