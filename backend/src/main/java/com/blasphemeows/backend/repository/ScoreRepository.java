package com.blasphemeows.backend.repository;

import com.blasphemeows.backend.model.Score;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScoreRepository extends JpaRepository<Score, Long> {

    List<Score> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT s FROM Score s JOIN FETCH s.user ORDER BY s.score DESC, s.createdAt ASC")
    List<Score> findLeaderboard(Pageable pageable);
}
