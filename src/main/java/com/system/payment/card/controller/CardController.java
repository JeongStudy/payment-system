package com.system.payment.card.controller;

import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.card.model.request.InicisRequest;
import com.system.payment.card.model.response.InicisBillingKeyResponse;
import com.system.payment.card.model.response.PGAuthParamsResponse;
import com.system.payment.card.model.response.PaymentUserCardResponse;
import com.system.payment.card.service.CardService;
import com.system.payment.card.service.InicisService;
import com.system.payment.provider.AuthUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/payment/cards")
@RequiredArgsConstructor
@Slf4j
public class CardController {

    private final CardService cardService;
    private final InicisService inicisService;
    private final AuthUserProvider authUserProvider;

    @GetMapping("/active")
    public List<PaymentUserCardResponse> getActiveCards() {
        Integer userId = authUserProvider.getUserId();
        return cardService.getActiveCards(userId);
    }

    @PostMapping("/auth")
    public ResponseEntity<PGAuthParamsResponse> getBillingAuthParams(@RequestBody CardAuthRequest request) {
        Integer userId = authUserProvider.getUserId();
        PGAuthParamsResponse response = cardService.getBillingAuthParams(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/inicis/return")
    public ResponseEntity<Void> handleAuthCallback(@ModelAttribute InicisRequest request) {
        String resultCode = inicisService.createAndSaveBillingKey(request);

        // 현재 도메인을 기준으로 리다이렉트 URL 생성
        String base = ServletUriComponentsBuilder.fromCurrentContextPath()
                .build().toUriString();
        String target = UriComponentsBuilder
                .fromUriString(base) // 문자열 기반이지만 프로토콜+호스트가 포함된 URI
                .path("/")
                .queryParam("resultCode", resultCode)
                .build()
                .toUriString();

        // 303 See Other 권장 (POST 이후 GET로 전환)
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(target))
                .build();
    }
}
