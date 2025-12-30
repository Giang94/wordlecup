package com.app.wordlecup.repo;

import com.app.wordlecup.model.RecentGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecentGameRepository extends JpaRepository<RecentGame, Long> {

    long count();

    Optional<RecentGame> findFirstByOrderByUsedAtAsc();

    Optional<RecentGame> findTopByOrderByUsedAtDesc();

    List<RecentGame> findAllByOrderByUsedAtAsc();
}
