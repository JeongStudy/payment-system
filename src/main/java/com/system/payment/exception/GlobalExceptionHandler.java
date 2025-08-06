package com.system.payment.exception;

import com.system.payment.util.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = {"com.system.payment"})
public class GlobalExceptionHandler {

	@ExceptionHandler(PaymentServerUnauthorizedException.class)
	public ResponseEntity<Response<Void>> handlePaymentServerUnauthorizedException(PaymentServerUnauthorizedException e) {
		ErrorCode errorCode = e.getErrorCode();
		Response<Void> response = Response.<Void>builder()
				.status(errorCode.getStatus())
				.message(errorCode.getMessage())
				.build();
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	@ExceptionHandler(PaymentServerBadRequestException.class)
	public ResponseEntity<Response<Void>> handlePaymentServerBadRequestException(PaymentServerBadRequestException e) {
		ErrorCode errorCode = e.getErrorCode();
		Response<Void> response = Response.<Void>builder()
				.status(errorCode.getStatus())
				.message(errorCode.getMessage())
				.build();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(PaymentServerNotFoundException.class)
	public ResponseEntity<Response<Void>> handlePaymentServerNotFoundException(PaymentServerNotFoundException e) {
		ErrorCode errorCode = e.getErrorCode();
		Response<Void> response = Response.<Void>builder()
				.status(errorCode.getStatus())
				.message(errorCode.getMessage())
				.build();
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	@ExceptionHandler(PaymentServerConflictException.class)
	public ResponseEntity<Response<Void>> handlePaymentServerConflictException(PaymentServerConflictException e) {
		ErrorCode errorCode = e.getErrorCode();
		Response<Void> response = Response.<Void>builder()
				.status(errorCode.getStatus())
				.message(errorCode.getMessage())
				.build();
		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Response<Void>> handleValidationExceptions(MethodArgumentNotValidException e) {
		BindingResult bindingResult = e.getBindingResult();
		Response<Void> response = Response.<Void>builder()
				.status(ErrorCode.BAD_REQUEST_PARAM.getStatus())
				.message(ErrorCode.BAD_REQUEST_PARAM.getMessage())
				.errors(Response.Error.of(bindingResult))
				.build();
		return ResponseEntity.badRequest().body(response);
	}

	// validation 실패 (Form 등)
	@ExceptionHandler(BindException.class)
	public ResponseEntity<Response<Void>> handleBindException(BindException e) {
		BindingResult bindingResult = e.getBindingResult();
		Response<Void> response = Response.<Void>builder()
				.status(ErrorCode.BAD_REQUEST_PARAM.getStatus())
				.message(ErrorCode.BAD_REQUEST_PARAM.getMessage())
				.errors(Response.Error.of(bindingResult))
				.build();
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(CryptoException.class)
	public ResponseEntity<Response<Void>> handleRsaKeyGenerateException(CryptoException e) {
		log.error("CryptoException 발생", e);
		ErrorCode errorCode = e.getErrorCode();

		Response<Void> response = Response.<Void>builder()
				.status(errorCode.getStatus())
				.message(errorCode.getMessage())
				.build();

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}

	// IllegalArgumentException 처리 (직접 throw 등)
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Response<Void>> handleIllegalArgument(IllegalArgumentException e) {
		log.error("IllegalArgumentException 발생", e);
		Response<Void> response = Response.<Void>builder()
				.status(ErrorCode.BAD_REQUEST_PARAM.getStatus())
				.message(e.getMessage())
				.build();
		return ResponseEntity.badRequest().body(response);
	}

	// NullPointerException만 따로 분리
	@ExceptionHandler(NullPointerException.class)
	public ResponseEntity<Response<Void>> handleNullPointerException(NullPointerException e) {
		log.error("NullPointerException 발생", e);
		Response<Void> response = Response.<Void>builder()
				.status(ErrorCode.SERVER_NULL_POINTER_ERROR.getStatus())
				.message(ErrorCode.SERVER_NULL_POINTER_ERROR.getMessage())
				.build();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Response<Void>> handleGeneralException(Exception e) {
		log.error("서버 내부 일반 오류 발생", e);
		Response<Void> response = Response.<Void>builder()
				.status(ErrorCode.SERVER_ERROR.getStatus())
				.message(e.getMessage() != null ? e.getMessage() : ErrorCode.SERVER_ERROR.getMessage())
				.build();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}

}