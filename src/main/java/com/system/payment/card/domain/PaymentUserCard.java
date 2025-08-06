package com.system.payment.card.domain;

import com.system.payment.common.domain.BaseEntity;
import com.system.payment.user.domain.PaymentUser;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Entity
@Table(name = "payment_user_card", schema = "payment")
public class PaymentUserCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private PaymentUser user;

    @Column(length = 4, nullable = false)
    private String cardNumberMasked;

    @Column(length = 50, nullable = false)
    private String cardCompany;

    @Column(length = 20, nullable = false)
    private String cardType;

    @Column(length = 4, nullable = false)
    private String expirationYear;

    @Column(length = 2, nullable = false)
    private String expirationMonth;

    @Column(length = 30, nullable = false)
    private String pgCompany;

    @Column(length = 20, nullable = false)
    private String pgCompanyCode;

    @Column(length = 200, nullable = false)
    private String billingKey;

    @Column(length = 20, nullable = false)
    private String billingKeyStatus;
}
