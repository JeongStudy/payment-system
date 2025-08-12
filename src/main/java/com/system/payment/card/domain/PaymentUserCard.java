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

    @Column(length = 200, nullable = false, unique = true)
    private String oid;

    @Column(length = 20)
    private String cardNumberMasked;

    @Column(length = 50)
    private String cardCompany;

    @Column(length = 20)
    private String cardType;

    @Column(length = 4)
    private String expirationYear;

    @Column(length = 2)
    private String expirationMonth;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PgCompany pgCompany;

    @Column(length = 20)
    private String pgCompanyCode;

    @Column(length = 200)
    private String billingKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private BillingKeyStatus billingKeyStatus;

    public void updateInicisCard(
            String cardNumberMasked, String cardType, String cardCode,
            String billingKey, BillingKeyStatus billingKeyStatus
    ) {
        if (this.billingKeyStatus != BillingKeyStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 발급 완료 전환 가능");
        }
        this.cardNumberMasked = cardNumberMasked;
        this.cardType = cardType;
        this.cardCompany = cardCode;
        this.pgCompany = PgCompany.INICIS;
        this.pgCompanyCode = PgCompany.INICIS.getCode();
        this.billingKey = billingKey;
        this.billingKeyStatus = billingKeyStatus;
        this.expirationYear = null;
        this.expirationMonth = null;
    }
}
