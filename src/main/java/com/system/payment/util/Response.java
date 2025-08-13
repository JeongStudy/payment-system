package com.system.payment.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
//@JsonInclude(JsonInclude.Include.NON_NULL) // errors가 null이면 포함하지 않음
public class Response<T> {

	private int status;
	private String message;
	private T data;
	private List<Error> errors;

	public static <T> ResponseEntity<Response<T>> ok(SuccessCode code) {
		Response<T> response = Response.<T>builder()
				.status(code.getStatus())
				.message(code.getMessage()).build();
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	public static <T> ResponseEntity<Response<T>> ok(T data, SuccessCode code) {
		Response<T> response = Response.<T>builder()
				.status(code.getStatus())
				.message(code.getMessage())
				.data(data).build();
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	public static <T> ResponseEntity<Response<T>> ok(T data, SuccessCode code, HttpHeaders httpHeaders) {
		Response<T> response = Response.<T>builder()
				.status(code.getStatus())
				.message(code.getMessage())
				.data(data).build();
		return ResponseEntity.status(HttpStatus.OK).headers(httpHeaders).body(response);
	}

	public static <T> ResponseEntity<Response<T>> created(SuccessCode code) {
		Response<T> response = Response.<T>builder()
				.status(code.getStatus())
				.message(code.getMessage()).build();
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	public static <T> ResponseEntity<Response<T>> created(T data, SuccessCode code) {
		Response<T> response = Response.<T>builder()
				.status(code.getStatus())
				.message(code.getMessage())
				.data(data).build();
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}


	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Error {
		private String field;
		private String value;
		private String reason;

		public static List<Error> of(BindingResult bindingResult) {
			List<FieldError> fieldErrors = bindingResult.getFieldErrors();
			return fieldErrors.stream()
					.map(error -> new Error(
							error.getField(),
							error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
							error.getDefaultMessage()))
					.collect(Collectors.toList());
		}
	}
}
