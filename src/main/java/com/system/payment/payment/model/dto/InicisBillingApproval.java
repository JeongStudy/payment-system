package com.system.payment.payment.model.dto;

import com.system.payment.payment.model.request.CreatePaymentRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class InicisBillingApproval {
	private final String mid;

	private final String type;

	private final String paymethod;

	private final String timestamp; //YYYYMMDDhhmmss

	private final String clientIp;

	private final String hashData;

	private Data data;

	@Getter
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Data {
		private String url;
		private String moid;
		private String goodName;
		private String buyerName;
		private String buyerEmail;
		private String buyerTel;
		private String price;
		private String billKey;
		private final String authentification = "00";

		public static Data create(
				String url, String moid, String goodName, String buyerName,
				String buyerEmail, String buyerTel, String price, String billKey
		) {
			return new Data(url, moid, goodName, buyerName, buyerEmail,
					buyerTel, price, billKey);
		}
	}


	public static InicisBillingApproval create(
			String mid, String type, String paymethod, String timestamp, String clientIp, String hashData,
			Data data
	) {

		return InicisBillingApproval.builder()
				.mid(mid)
				.type(type)
				.paymethod(paymethod)
				.timestamp(timestamp)
				.clientIp(clientIp)
				.hashData(hashData)
				.data(data)
				.build();
	}
}
