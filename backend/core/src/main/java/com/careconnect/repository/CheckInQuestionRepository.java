package com.careconnect.repository;

import com.careconnect.model.CheckInQuestion;
import com.careconnect.model.CheckInQuestionId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import com.careconnect.dto.QuestionDTO;

import java.util.List;

public interface CheckInQuestionRepository extends JpaRepository<CheckInQuestion, CheckInQuestionId> {
    List<CheckInQuestion> findByCheckIn_IdOrderByOrdinalAsc(Long checkInId);
    boolean existsByCheckIn_IdAndQuestion_Id(Long checkInId, Long questionId);

    @Query("""
            SELECT new com.careconnect.dto.QuestionDTO(
                q.id,
                ciq.promptSnapshot,
                ciq.typeSnapshot,
                ciq.required,
                true,
                ciq.ordinal
            )
            FROM CheckInQuestion ciq
            JOIN ciq.question q
            WHERE ciq.checkIn.id = :checkInId
            ORDER BY ciq.ordinal ASC
            """)
    List<QuestionDTO> findSnapshotQuestionDtosByCheckInId(@Param("checkInId") Long checkInId);
}
