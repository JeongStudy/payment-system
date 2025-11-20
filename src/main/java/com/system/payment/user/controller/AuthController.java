package com.system.payment.user.controller;

import com.system.payment.user.model.reponse.LoginResponse;
import com.system.payment.user.model.reponse.UserResponse;
import com.system.payment.user.model.request.LoginRequest;
import com.system.payment.user.model.request.SignUpRequest;
import com.system.payment.user.service.AuthService;
import com.system.payment.user.service.UserService;
import com.system.payment.common.dto.response.Response;
import com.system.payment.common.dto.response.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
		return Response.created(SuccessCode.USER_CREATED);
	}

	@PostMapping("/login")
	public ResponseEntity<Response<Void>> login(@Valid @RequestBody LoginRequest request) {
		final LoginResponse loginResponse = authService.login(request);

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(loginResponse.getToken());
		return Response.ok(null, SuccessCode.USER_LOGIN_SUCCESS, headers);
	}

	@GetMapping("/info")
	public ResponseEntity<Response<UserResponse>> getMyUserInfo() {
		return Response.ok(userService.getUser(), SuccessCode.OK);
	}
}