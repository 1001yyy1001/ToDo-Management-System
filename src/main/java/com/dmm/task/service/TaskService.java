package com.dmm.task.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dmm.task.data.entity.Task;
import com.dmm.task.data.repository.TaskRepository;

@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;

    public List<Task> findTasksByDateBetween(LocalDateTime from, LocalDateTime to, String name) {
        return taskRepository.findByDateBetween(from, to, name);
    }

    public void saveTask(Task task) {
        taskRepository.save(task);
    }

    public void deleteTask(int id) {
        taskRepository.deleteById(id);
    }

    public Task getTaskById(int id) {
        return taskRepository.findById(id).orElse(null);
    }
}
