package com.careconnect.dto;

import java.math.BigDecimal;

public record AnswerUpsertRequestDTO(
        Long questionId,
        String valueText,
        Boolean valueBoolean,
        BigDecimal valueNumber
) {
}
