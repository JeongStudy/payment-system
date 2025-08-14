package com.system.payment.payment.controller;

import com.system.payment.payment.model.request.CreatePaymentRequest;
import com.system.payment.payment.model.response.CreatePaymentResponse;
import com.system.payment.payment.model.response.IdempotencyKeyResponse;
import com.system.payment.payment.service.PaymentRequestService;
import com.system.payment.util.Response;
import com.system.payment.util.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentRequestController {

    private final PaymentRequestService paymentRequestService;

    @PostMapping("/requests")
    public ResponseEntity<Response<CreatePaymentResponse>> create(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        final CreatePaymentResponse createPaymentResponse = paymentRequestService.createAndPublish(request);
        return Response.created(createPaymentResponse, SuccessCode.PAYMENT_REQUESTS_SUCCESS);
    }
	@GetMapping("/requests/idempotency-key")
	public ResponseEntity<Response<IdempotencyKeyResponse>> getIdempotencyKey() {
        final IdempotencyKeyResponse idempotencyKeyResponse = paymentRequestService.getIdempotencyKey();
        return Response.ok(idempotencyKeyResponse,SuccessCode.PAYMENT_IDEMPOTENCY_KEY_SUCCESS);
	}

//    @GetMapping("/requests/{id}")
//    public ResponseEntity<Response<PaymentRequestResponse>> get(@PathVariable Long id) {
//        PaymentRequest pr = paymentRequestService.getByIdOrThrow(id);
//        return ResponseEntity.ok(Response.ok(PaymentRequestResponse.from(pr)));
//    }
}
