package com.system.payment.payment.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Example {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
}
