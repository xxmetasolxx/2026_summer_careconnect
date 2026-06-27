package com.careconnect.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitAnswersRequestDTO(
        @NotEmpty(message = "answers must not be empty")
        List<@Valid AnswerUpsertRequestDTO> answers
) {
}
