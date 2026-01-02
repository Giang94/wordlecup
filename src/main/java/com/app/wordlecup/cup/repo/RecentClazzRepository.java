package com.app.wordlecup.cup.repo;

import com.app.wordlecup.cup.entity.RecentClazz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecentClazzRepository extends JpaRepository<RecentClazz, Long> {

    long count();

    Optional<RecentClazz> findFirstByOrderByUsedAtAsc();

    Optional<RecentClazz> findTopByOrderByUsedAtDesc();

    List<RecentClazz> findAllByOrderByUsedAtAsc();
}
