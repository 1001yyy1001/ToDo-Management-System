package com.dmm.task.data.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dmm.task.data.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Integer> {
    @Query("select t from Task t where t.date between :from and :to and t.name = :name")
    List<Task> findByDateBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("name") String name);
}
