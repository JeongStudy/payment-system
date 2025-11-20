package com.system.payment.common.exception;

import com.system.payment.util.Response;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
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
		 return Response.unauthorized(e.getErrorCode());
	}

	@ExceptionHandler(PaymentServerBadRequestException.class)
	public ResponseEntity<Response<Void>> handlePaymentServerBadRequestException(PaymentServerBadRequestException e) {
		return Response.badRequest(e.getErrorCode());
	}

	@ExceptionHandler(PaymentServerNotFoundException.class)
	public ResponseEntity<Response<Void>> handlePaymentServerNotFoundException(PaymentServerNotFoundException e) {
		return Response.notFound(e.getErrorCode());
	}

	@ExceptionHandler(PaymentServerConflictException.class)
	public ResponseEntity<Response<Void>> handlePaymentServerConflictException(PaymentServerConflictException e) {
		return Response.conflict(e.getErrorCode());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Response<Void>> handleValidationExceptions(MethodArgumentNotValidException e) {
		BindingResult bindingResult = e.getBindingResult();
		return Response.badRequest(ErrorCode.BAD_REQUEST_PARAM, bindingResult);
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<Response<Void>> handleBindException(BindException e) {
		BindingResult bindingResult = e.getBindingResult();
		return Response.badRequest(ErrorCode.BAD_REQUEST_PARAM, bindingResult);
	}

	@ExceptionHandler(CryptoException.class)
	public ResponseEntity<Response<Void>> handleRsaKeyGenerateException(CryptoException e) {
		log.error("CryptoException 발생", e);
		return Response.internalServerError(e.getErrorCode());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Response<Void>> handleIllegalArgument(IllegalArgumentException e) {
		log.error("IllegalArgumentException 발생", e);
		return Response.badRequest(ErrorCode.BAD_REQUEST_PARAM);
	}

	@ExceptionHandler(JwtException.class)
	public ResponseEntity<Response<Void>> handleJwtException(JwtException e) {
		log.error("JwtException 발생", e);
		return Response.internalServerError(ErrorCode.SERVER_ERROR);
	}

	@ExceptionHandler(NullPointerException.class)
	public ResponseEntity<Response<Void>> handleNullPointerException(NullPointerException e) {
		log.error("NullPointerException 발생", e);
		return Response.internalServerError(ErrorCode.SERVER_NULL_POINTER_ERROR);
	}

	@ExceptionHandler(PgResponseParseException.class)
	public ResponseEntity<Response<Void>> handlePgResponseParseException(PgResponseParseException e) {
		log.error("PG 응답 파싱 실패", e);
		return Response.internalServerError(e.getErrorCode());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Response<Void>> handleGeneralException(Exception e) {
		log.error("서버 내부 일반 오류 발생", e);
		return Response.internalServerError(ErrorCode.SERVER_ERROR);
	}

}