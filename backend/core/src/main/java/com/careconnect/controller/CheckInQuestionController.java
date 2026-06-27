package com.careconnect.controller;

import com.careconnect.security.Permission;
import com.careconnect.security.RequirePermission;

import com.careconnect.dto.CheckInCreateRequestDTO;
import com.careconnect.dto.CheckInCreateResponseDTO;
import com.careconnect.dto.QuestionDTO;
import com.careconnect.service.CheckInSnapshotService;
import com.careconnect.service.QuestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.careconnect.util.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = {"/api/checkins", "/v1/api/checkins"})
public class CheckInQuestionController {

    private final QuestionService questionService;
    private final CheckInSnapshotService checkInSnapshotService;
    private final SecurityUtil securityUtil;

    public CheckInQuestionController(
            QuestionService questionService,
            CheckInSnapshotService checkInSnapshotService,
            SecurityUtil securityUtil
    ) {
        this.questionService = questionService;
        this.checkInSnapshotService = checkInSnapshotService;
        this.securityUtil = securityUtil;
    }

    /**
     * GET /api/checkins/{checkInId}/questions
     * GET /v1/api/checkins/{checkInId}/questions
     */
    @RequirePermission(Permission.VIEW_ASSIGNED_PATIENTS)

    @GetMapping("/{checkInId}/questions")
    public ResponseEntity<List<QuestionDTO>> getQuestions(
            @PathVariable("checkInId") Long checkInId,
            HttpServletRequest request
    ) {
        // RBAC: Defense-in-depth — verify caller is a real user in the database
        securityUtil.resolveCurrentUser();

        String uri = request.getRequestURI();
        if (uri.startsWith("/v1/api/")) {
            // Backward compatibility for legacy clients.
            return ResponseEntity.ok(questionService.findActiveOrdered());
        }
        List<QuestionDTO> questions = checkInSnapshotService.getSnapshotQuestions(checkInId);
        return ResponseEntity.ok(questions);
    }

    /**
     * POST /api/checkins
     * POST /v1/api/checkins
     */
    @RequirePermission(Permission.CREATE_TASKS)
    @PostMapping
    public ResponseEntity<CheckInCreateResponseDTO> createCheckIn(
            @Valid @RequestBody CheckInCreateRequestDTO request
    ) {
        securityUtil.resolveCurrentUser();
        CheckInCreateResponseDTO created = checkInSnapshotService.createCheckInWithSnapshot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
