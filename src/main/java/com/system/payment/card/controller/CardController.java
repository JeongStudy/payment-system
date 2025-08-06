package com.system.payment.card.controller;

import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.card.model.request.InicisRequest;
import com.system.payment.card.model.response.InicisBillingKeyResponse;
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
    public void handleInicisReturn(@ModelAttribute InicisRequest request, HttpServletResponse response) throws IOException {
//        log.info(request.toString());

        InicisBillingKeyResponse billingKeyResponse = cardService.getInicisBillingKey(request);

        log.info(billingKeyResponse.toString());

        // 처리 후 프론트로 이동
//        String url = "https://9892b382ce09.ngrok-free.app/card/return?resultCode=" + resultCode + "&authToken=" + authToken;
//        response.sendRedirect(url);
    }
}
