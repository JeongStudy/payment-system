package com.system.payment.payment.service;

import com.system.payment.card.domain.entity.PaymentUserCard;
import com.system.payment.card.repository.PaymentUserCardRepository;
import com.system.payment.common.exception.PaymentServerConflictException;
import com.system.payment.common.exception.PaymentServerNotFoundException;
import com.system.payment.outbox.service.OutboxService;
import com.system.payment.payment.domain.entity.Payment;
import com.system.payment.payment.domain.entity.PaymentDetail;
import com.system.payment.payment.model.request.CreatePaymentRequest;
import com.system.payment.payment.model.response.CreatePaymentResponse;
import com.system.payment.payment.repository.PaymentRepository;
import com.system.payment.user.domain.AesKey;
import com.system.payment.user.domain.PaymentUser;
import com.system.payment.user.repository.PaymentUserRepository;
import com.system.payment.user.service.CredentialService;
import com.system.payment.user.service.CryptoService;
import com.system.payment.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class PaymentRequestServiceTest {

	@Mock
	UserService userService;
	@Mock
	PaymentUserRepository paymentUserRepository;
	@Mock
	CryptoService cryptoService;
	@Mock
	CredentialService credentialService;
	@Mock
	PaymentHistoryService paymentHistoryService;
	@Mock
	OutboxService outboxService;
	@Mock
	PaymentUserCardRepository paymentUserCardRepository;
	@Mock
	PaymentRepository paymentRepository;

	@InjectMocks
	PaymentRequestService paymentRequestService;


	@BeforeEach
	void setUp() {
	}

	private CreatePaymentRequest createPaymentRequest() {
		CreatePaymentRequest createPaymentRequest = CreatePaymentRequest.builder()
				.paymentUserCardId(1)
				.serviceOrderId("2400811")
				.productName("AI 라이센스 키(연 1석)")
				.idempotencyKey("idem-123")
				.amount(1)
				.rsaPublicKey("rsa")
				.encAesKey("enc-aes")
				.encPassword("enc-pw")
				.build();
		return createPaymentRequest;
	}

	private CreatePaymentRequest createPaymentRequest(Integer amount) {
		CreatePaymentRequest createPaymentRequest = CreatePaymentRequest.builder()
				.paymentUserCardId(1)
				.serviceOrderId("2400811")
				.productName("AI 라이센스 키(연 1석)")
				.idempotencyKey("idem-123")
				.amount(amount)
				.rsaPublicKey("rsa")
				.encAesKey("enc-aes")
				.encPassword("enc-pw")
				.build();
		return createPaymentRequest;
	}

	@Test
	@DisplayName("결제 요청 발급 성공")
	void createPaymentAndPublish_success() {
		// given
		when(paymentRepository.existsByIdempotencyKey("idem-123")).thenReturn(false);

		PaymentUser mockUser = mock(PaymentUser.class);
		when(mockUser.getId()).thenReturn(1);
		when(mockUser.getEncPassword()).thenReturn("$2b$hash");
		when(userService.findUser()).thenReturn(mockUser);

		PaymentUserCard mockCard = mock(PaymentUserCard.class);
		when(mockCard.getId()).thenReturn(1);
		when(paymentUserCardRepository.findById(1)).thenReturn(Optional.of(mockCard));

		// 암호 스텁 스킵
		AesKey aesKey = mock(AesKey.class);
		when(cryptoService.resolveValidAesKey("rsa", "enc-aes")).thenReturn(aesKey);
		when(aesKey.getAesKey()).thenReturn("AES-KEY");
		when(cryptoService.decryptPasswordWithAes("enc-pw", "AES-KEY")).thenReturn("plain");
		doNothing().when(credentialService).verifyOrThrow("plain", "$2b$hash");

		ArgumentCaptor<Payment> paymentArg = ArgumentCaptor.forClass(Payment.class);
		when(paymentRepository.save(paymentArg.capture())).thenAnswer(inv -> paymentArg.getValue());

		// when
		final CreatePaymentRequest paymentRequest = createPaymentRequest();
		CreatePaymentResponse res =
				paymentRequestService.createPaymentAndPublish(paymentRequest);

		// then
		assertThat(res).isNotNull();

		Payment toSave = paymentArg.getValue();

		assertThat(toSave.getDetails()).isNotNull().isNotEmpty();
		int sum = toSave.getDetails().stream().mapToInt(PaymentDetail::getAmount).sum();
		assertThat(sum).isEqualTo(toSave.getTotalAmount());
		assertThat(toSave.getTransactionId()).isNotBlank();

		verify(paymentHistoryService).recordCreated(same(toSave));

		verify(outboxService).enqueuePaymentRequested(
				any(),
				eq(toSave.getTransactionId()),
				eq(toSave.getUserRef().getUserId()),
				eq("CARD"),
				eq(paymentRequest.getPaymentUserCardId()),
				eq(paymentRequest.getProductName())
		);
	}

	@Test
	@DisplayName("결제 요청 멱등성 키 중복 실패")
	void duplicate_idempotencyKey_conflict() {
		when(paymentRepository.existsByIdempotencyKey("idem-123")).thenReturn(true);

		final CreatePaymentRequest paymentRequest = createPaymentRequest();

		assertThatThrownBy(() -> paymentRequestService.createPaymentAndPublish(paymentRequest))
				.isInstanceOf(PaymentServerConflictException.class);

		verifyNoInteractions(paymentHistoryService, outboxService);
		verifyNoInteractions(credentialService, cryptoService);
	}

	@Test
	@DisplayName("결제 요청 유저 카드 찾을 수 없음 실패")
	void card_not_found() {
		when(paymentRepository.existsByIdempotencyKey("idem-123")).thenReturn(false);

		PaymentUser user = mock(PaymentUser.class);
		lenient().when(user.getId()).thenReturn(1);
		lenient().when(user.getEncPassword()).thenReturn("$2b$hash");
		lenient().when(userService.findUser()).thenReturn(user);

		AesKey aesKey = mock(AesKey.class);
		lenient().when(cryptoService.resolveValidAesKey("rsa", "enc-aes")).thenReturn(aesKey);
		lenient().when(aesKey.getAesKey()).thenReturn("AES-KEY");
		lenient().when(cryptoService.decryptPasswordWithAes("enc-pw", "AES-KEY")).thenReturn("plain");
		doNothing().when(credentialService).verifyOrThrow("plain", "$2b$hash");

		when(paymentUserCardRepository.findById(1)).thenReturn(Optional.empty());

		final CreatePaymentRequest createPaymentRequest = createPaymentRequest();

		assertThatThrownBy(() -> paymentRequestService.createPaymentAndPublish(createPaymentRequest))
				.isInstanceOf(PaymentServerNotFoundException.class);

		verify(paymentRepository, never()).save(any());
		verifyNoInteractions(paymentHistoryService, outboxService);

	}
}
