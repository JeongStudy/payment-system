package com.system.payment.example.service;

import com.system.payment.example.model.response.ExampleResponse;
import com.system.payment.example.repository.ExampleRepository;
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
