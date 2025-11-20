package com.system.payment.user.domain;

import com.system.payment.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "payment_user", schema = "payment")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PaymentUser extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 200, nullable = false, unique = true)
	private String email;

	@Column(length = 300, nullable = false)
	private String encPassword;

	@Column(length = 50, nullable = false)
	private String lastName;

	@Column(length = 50, nullable = false)
	private String firstName;

	@Column(length = 11, nullable = false)
	private String phoneNumber;

	private PaymentUser(String email, String encodedPassword, String firstName, String lastName, String phoneNumber) {
		this.email = email;
		this.encPassword = encodedPassword;
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
