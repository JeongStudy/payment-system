package com.system.payment.user.domain.jaebin;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PaymentUser extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(nullable = false, length = 300)
    private String password;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "phone_number", length = 11)
    private String phoneNumber;

    @Column(name = "is_deleted", length = 1)
    private String isDeleted = "F";

    public PaymentUser(String email, String encodedPassword, String firstName, String lastName, String phoneNumber) {
		this.email = email;
        this.password = encodedPassword;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
	}

    public static PaymentUser create(String email, String encodedPassword, String firstName, String lastName, String phoneNumber) {
        return new PaymentUser(
				email,
				encodedPassword,
				firstName,
				lastName,
				phoneNumber
		);
    }
}
