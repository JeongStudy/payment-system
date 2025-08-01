package com.system.payment.user.controller;

import com.system.payment.user.model.reponse.AesKeyResponse;
import com.system.payment.user.model.request.SignUpRequest;
import com.system.payment.user.service.AuthService;
import com.system.payment.util.Response;
import com.system.payment.util.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<Response<Void>> signUp(@Valid @RequestBody SignUpRequest request) {
        authService.signUp(request);
        Response<Void> response = Response.<Void>builder()
				.status(SuccessCode.USER_CREATED.getStatus())
				.message(SuccessCode.USER_CREATED.getMessage())
				.build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}