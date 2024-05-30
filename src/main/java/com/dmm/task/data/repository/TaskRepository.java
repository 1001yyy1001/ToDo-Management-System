package com.dmm.task.data.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dmm.task.data.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Integer> {
    List<Task> findByDateBetweenAndName(LocalDateTime start, LocalDateTime end, String name);
    List<Task> findByDateBetween(LocalDateTime start, LocalDateTime end);
}
