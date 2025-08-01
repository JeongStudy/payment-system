package com.system.payment.payment.service;

import com.system.payment.payment.repository.ExampleRepository;
import com.system.payment.payment.model.response.ExampleResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExampleService {
	private final ExampleRepository exampleRepository;

	public ExampleService(ExampleRepository exampleRepository) {
		this.exampleRepository = exampleRepository;
	}

	public List<ExampleResponse> findAll() {
		return exampleRepository.findAll()
				.stream()
				.map(ExampleResponse::new)
				.collect(Collectors.toList());
	}

}
