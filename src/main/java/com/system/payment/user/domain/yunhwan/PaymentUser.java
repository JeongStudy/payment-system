package com.system.payment.user.domain.yunhwan;

import com.system.payment.common.domain.yunhwan.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Entity
@Table(name = "payment_user", schema = "payment")
public class PaymentUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 200, nullable = false, unique = true)
    private String email;

    @Column(length = 300, nullable = false)
    private String password;

    @Column(length = 50, nullable = false)
    private String lastName;

    @Column(length = 50, nullable = false)
    private String firstName;

    @Column(length = 11, nullable = false)
    private String phoneNumber;
}
