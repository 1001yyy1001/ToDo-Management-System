package com.dmm.task.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dmm.task.data.entity.Task;
import com.dmm.task.service.TaskService;

@Controller
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping("/main")
    public String calendar(@RequestParam(required = false) String date, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        LocalDate targetDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        LocalDateTime startOfMonth = targetDate.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = targetDate.withDayOfMonth(targetDate.lengthOfMonth()).atTime(LocalTime.MAX);
        List<Task> tasks = taskService.findTasksByDateBetween(startOfMonth, endOfMonth, userDetails.getUsername());

        model.addAttribute("tasks", tasks);
        model.addAttribute("month", targetDate.getMonthValue() + "月");
        model.addAttribute("prev", targetDate.minusMonths(1));
        model.addAttribute("next", targetDate.plusMonths(1));
        model.addAttribute("matrix", createCalendarMatrix(startOfMonth.toLocalDate()));

        return "calendar";
    }

    @GetMapping("/main/create")
    public String createTaskForm(Model model) {
        model.addAttribute("task", new Task());
        return "create";
    }

    @PostMapping("/main/create")
    public String createTask(Task task, @AuthenticationPrincipal UserDetails userDetails) {
        task.setName(userDetails.getUsername());
        task.setDone(false);
        taskService.saveTask(task);
        return "redirect:/main";
    }

    @GetMapping("/main/edit/{id}")
    public String editTaskForm(@PathVariable int id, Model model) {
        Task task = taskService.getTaskById(id);
        model.addAttribute("task", task);
        return "edit";
    }

    @PostMapping("/main/edit/{id}")
    public String editTask(@PathVariable int id, Task task) {
        Task existingTask = taskService.getTaskById(id);
        existingTask.setTitle(task.getTitle());
        existingTask.setDate(task.getDate());
        existingTask.setText(task.getText());
        existingTask.setDone(task.getDone());
        taskService.saveTask(existingTask);
        return "redirect:/main";
    }

    @PostMapping("/main/delete/{id}")
    public String deleteTask(@PathVariable int id) {
        taskService.deleteTask(id);
        return "redirect:/main";
    }

    private List<List<LocalDate>> createCalendarMatrix(LocalDate startOfMonth) {
        LocalDate start = startOfMonth.minusDays(startOfMonth.getDayOfWeek().getValue() % 7);
        Map<Integer, List<LocalDate>> groupedByWeek = Stream.iterate(start, date -> date.plusDays(1))
                .limit(42)
                .collect(Collectors.groupingBy(date -> date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)));
        return groupedByWeek.values().stream().collect(Collectors.toList());
    }
}
