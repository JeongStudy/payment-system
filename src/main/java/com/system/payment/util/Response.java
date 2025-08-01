package com.system.payment.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
