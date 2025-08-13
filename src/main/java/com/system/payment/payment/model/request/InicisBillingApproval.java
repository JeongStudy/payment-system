package com.system.payment.payment.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class InicisBillingApproval {
	private final Integer mid;

	private final String type = "billing";

	private final String paymethod = "card";

	//YYYYMMDDhhmmss
	private final String timestamp;

	private final String clientIp;

	private final String hashData;

	private Data data;

	@Getter
	@AllArgsConstructor
	@NoArgsConstructor
	public class Data{
		private String url;
		private String moid;
		private String goodName;
		private String buyerName;
		private String buyerEmail;
		private String buyerTel;
		private String price;
		private String billKey;
		private final String authentification = "00";

	}

	public InicisBillingApproval toInicisBillingApproval(CreatePaymentRequest request){
		return new InicisBillingApproval();
	}
}
