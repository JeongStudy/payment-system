package com.system.payment.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceRef {
	@Enumerated(EnumType.STRING)
	@Column(length = 20, nullable = true)
	private ReferenceType referenceType;

	@Column
	private Integer referenceId;

	public static ReferenceRef of(ReferenceType referenceType, Integer referenceId) {
        return ReferenceRef.builder()
                .referenceType(referenceType)
                .referenceId(referenceId).build();
    }
}