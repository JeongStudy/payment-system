package com.system.payment.card.controller;

import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.card.model.response.PGAuthParamsResponse;
import com.system.payment.card.service.CardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/payment/cards")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class CardController {

    private final CardService cardService;

    @PostMapping("/auth")
    public ResponseEntity<PGAuthParamsResponse> getBillingAuthParams(@RequestBody CardAuthRequest request) {
        PGAuthParamsResponse response = cardService.getBillingAuthParams(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/inicis/return")
    public void handleInicisReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 이니시스가 넘겨주는 파라미터 받기
        String resultCode = request.getParameter("resultCode");
        String authToken = request.getParameter("authToken");
        // ... 기타 처리, DB저장 등
        log.info(resultCode);
        log.info(authToken);

        // 처리 후 프론트로 이동
//        response.sendRedirect("/card/return?resultCode=" + resultCode + "&authToken=" + authToken);
    }
}
