package com.careconnect.controller;

import com.careconnect.dto.AnswerUpsertRequestDTO;
import com.careconnect.dto.CheckInCreateResponseDTO;
import com.careconnect.dto.QuestionDTO;
import com.careconnect.dto.SubmitAnswersRequestDTO;
import com.careconnect.dto.SubmitAnswersResponseDTO;
import com.careconnect.model.User;
import com.careconnect.security.Role;
import com.careconnect.service.AnswerSubmissionService;
import com.careconnect.service.CheckInSnapshotService;
import com.careconnect.service.QuestionService;
import com.careconnect.util.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CheckInQuestionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QuestionService questionService;
    @Mock
    private CheckInSnapshotService checkInSnapshotService;
    @Mock
    private AnswerSubmissionService answerSubmissionService;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private CheckInQuestionController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
        User user = new User();
        user.setId(100L);
        user.setRole(Role.CAREGIVER);
        when(securityUtil.resolveCurrentUser()).thenReturn(user);
    }

    @Test
    void getQuestions_primaryPath_returnsSnapshotQuestions() throws Exception {
        when(checkInSnapshotService.getSnapshotQuestions(1L))
                .thenReturn(List.of(new QuestionDTO(1L, "Snapshot", "TEXT", true, true, 1)));

        mockMvc.perform(get("/api/checkins/1/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].prompt").value("Snapshot"));

        verify(checkInSnapshotService).getSnapshotQuestions(1L);
        verify(questionService, never()).findActiveOrdered();
    }

    @Test
    void getQuestions_versionedPath_returnsLegacyActiveQuestions() throws Exception {
        when(questionService.findActiveOrdered())
                .thenReturn(List.of(new QuestionDTO(1L, "Legacy", "TEXT", true, true, 1)));

        mockMvc.perform(get("/v1/api/checkins/1/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].prompt").value("Legacy"));

        verify(questionService).findActiveOrdered();
        verify(checkInSnapshotService, never()).getSnapshotQuestions(any());
    }

    @Test
    void createCheckIn_returnsCreated() throws Exception {
        when(checkInSnapshotService.createCheckInWithSnapshot(any()))
                .thenReturn(new CheckInCreateResponseDTO(
                        11L,
                        7L,
                        OffsetDateTime.parse("2026-06-26T10:00:00Z"),
                        2
                ));

        mockMvc.perform(post("/api/checkins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":7,\"selectedQuestionIds\":[1,2]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.checkInId").value(11));
    }

    @Test
    void submitAnswers_returnsCreated() throws Exception {
        SubmitAnswersRequestDTO request = new SubmitAnswersRequestDTO(List.of(
                new AnswerUpsertRequestDTO(1L, "ok", null, null),
                new AnswerUpsertRequestDTO(2L, null, true, null),
                new AnswerUpsertRequestDTO(3L, null, null, new BigDecimal("7.5"))
        ));
        when(answerSubmissionService.submitAnswers(any(), any()))
                .thenReturn(new SubmitAnswersResponseDTO(
                        1L,
                        3,
                        OffsetDateTime.parse("2026-06-26T10:30:00Z")
                ));

        mockMvc.perform(post("/api/checkins/1/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.checkInId").value(1))
                .andExpect(jsonPath("$.acceptedAnswerCount").value(3));

        verify(answerSubmissionService).submitAnswers(any(), any());
    }
}
