package com.system.payment.card.controller;

import com.system.payment.card.model.request.CardAuthRequest;
import com.system.payment.card.model.response.PGAuthParamsResponse;
import com.system.payment.card.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment/cards")
@RequiredArgsConstructor
@Slf4j
public class CardController {

    private final CardService cardService;

    @PostMapping("/auth")
    public ResponseEntity<PGAuthParamsResponse> getBillingAuthParams(@RequestBody CardAuthRequest request) {
        PGAuthParamsResponse response = cardService.getBillingAuthParams(request);
        return ResponseEntity.ok(response);
    }
}
