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

	// 커스텀 비즈니스 예외 처리
    @ExceptionHandler(PaymentServerException.class)
    public ResponseEntity<Response<Void>> handlePaymentServerException(PaymentServerException e) {
        ErrorCode errorCode = e.getErrorCode();
        Response<Void> response = Response.<Void>builder()
            .status(errorCode.getStatus())
            .message(errorCode.getMessage())
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

	// validation 실패 (RequestBody DTO @Valid 등)
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

	@ExceptionHandler(RsaKeyGenerateException.class)
	public ResponseEntity<Response<Void>> handleRsaKeyGenerateException(RsaKeyGenerateException e) {
		log.error("RsaKeyGenerateException 발생", e);
		ErrorCode errorCode = e.getErrorCode();

		Response<Void> response = Response.<Void>builder()
				.status(errorCode.getStatus())
				.message(errorCode.getMessage())
				.build();

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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

	// 그 외의 모든 Exception 처리
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