package com.system.payment.payment.model.response;

import com.system.payment.payment.domain.Example;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExampleResponse {
	private Long id;
	private String name;

	public ExampleResponse(Example example) {
		this.id = example.getId();
		this.name = example.getName();
	}
}
