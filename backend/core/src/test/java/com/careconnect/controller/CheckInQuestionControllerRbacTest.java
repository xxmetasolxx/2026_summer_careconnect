package com.careconnect.controller;

import com.careconnect.dto.QuestionDTO;
import com.careconnect.dto.SubmitAnswersResponseDTO;
import com.careconnect.model.User;
import com.careconnect.security.Role;
import com.careconnect.service.AnswerSubmissionService;
import com.careconnect.service.CheckInSnapshotService;
import com.careconnect.service.QuestionService;
import com.careconnect.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckInQuestionController.class)
@DisplayName("CheckInQuestionController RBAC Tests")
class CheckInQuestionControllerRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionService questionService;
    @MockitoBean
    private CheckInSnapshotService checkInSnapshotService;
    @MockitoBean
    private AnswerSubmissionService answerSubmissionService;
    @MockitoBean
    private SecurityUtil securityUtil;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(Role.ADMIN);
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    void unknownUser_failsBeforeServiceAccess() throws Exception {
        when(securityUtil.resolveCurrentUser())
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/api/checkins/1/questions"))
                .andExpect(status().isInternalServerError());

        verifyNoInteractions(questionService, checkInSnapshotService, answerSubmissionService);
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    void authenticatedUser_canReadQuestions() throws Exception {
        when(securityUtil.resolveCurrentUser()).thenReturn(adminUser);
        when(checkInSnapshotService.getSnapshotQuestions(1L))
                .thenReturn(List.of(new QuestionDTO(1L, "Q", "TEXT", true, true, 1)));

        mockMvc.perform(get("/api/checkins/1/questions"))
                .andExpect(status().isOk());

        verify(checkInSnapshotService).getSnapshotQuestions(1L);
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    void authenticatedUser_canSubmitAnswers() throws Exception {
        when(securityUtil.resolveCurrentUser()).thenReturn(adminUser);
        when(answerSubmissionService.submitAnswers(any(), any()))
                .thenReturn(new SubmitAnswersResponseDTO(1L, 1, OffsetDateTime.now()));

        mockMvc.perform(post("/api/checkins/1/answers")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"answers\":[{\"questionId\":1,\"valueText\":\"ok\"}]}"))
                .andExpect(status().isCreated());

        verify(answerSubmissionService).submitAnswers(any(), any());
    }
}
