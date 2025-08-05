package com.system.payment.user.controller;

import com.system.payment.user.model.reponse.LoginResponse;
import com.system.payment.user.model.reponse.UserResponse;
import com.system.payment.user.model.request.LoginRequest;
import com.system.payment.user.model.request.SignUpRequest;
import com.system.payment.user.service.AuthService;
import com.system.payment.user.service.UserService;
import com.system.payment.util.Response;
import com.system.payment.util.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final UserService userService;

	@PostMapping("/signup")
	public ResponseEntity<Response<Void>> signUp(@Valid @RequestBody SignUpRequest request) {
		authService.signUp(request);
		Response<Void> response = Response.<Void>builder()
				.status(SuccessCode.USER_CREATED.getStatus())
				.message(SuccessCode.USER_CREATED.getMessage())
				.build();
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/login")
	public ResponseEntity<Response<Void>> login(@RequestBody LoginRequest request) {
		final LoginResponse loginResponse = authService.login(request);

		Response<Void> response = Response.<Void>builder()
				.status(SuccessCode.USER_LOGIN_SUCCESS.getStatus())
				.message(SuccessCode.USER_LOGIN_SUCCESS.getMessage())
				.build();

		return ResponseEntity.status(HttpStatus.CREATED)
				.header("Authorization", "Bearer " + loginResponse.getToken())
				.body(response);
	}

	@GetMapping("/info")
	public ResponseEntity<Response<UserResponse>> getMyUserInfo() {
		Response<UserResponse> response = Response.<UserResponse>builder()
				.status(SuccessCode.OK.getStatus())
				.message(SuccessCode.OK.getMessage())
				.data(userService.getUser())
				.build();
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}