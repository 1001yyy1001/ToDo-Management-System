package com.dmm.task.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy年M月");

    @GetMapping("/home")
    public String calendar(@RequestParam(required = false) String date, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        LocalDate targetDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        LocalDateTime startOfMonth = targetDate.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = targetDate.withDayOfMonth(targetDate.lengthOfMonth()).atTime(LocalTime.MAX);
        List<Task> tasks;

        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            tasks = taskService.findTasksByDateBetween(startOfMonth, endOfMonth);
        } else {
            tasks = taskService.findTasksByDateBetween(startOfMonth, endOfMonth, userDetails.getUsername());
        }

        // 日付でタスクをマッピングする
        Map<LocalDate, List<Task>> tasksByDate = tasks.stream()
            .collect(Collectors.groupingBy(task -> task.getDate().toLocalDate()));

        model.addAttribute("tasks", tasksByDate);
        model.addAttribute("month", targetDate.format(YEAR_MONTH_FORMATTER));
        model.addAttribute("prev", targetDate.minusMonths(1));
        model.addAttribute("next", targetDate.plusMonths(1));
        model.addAttribute("matrix", createCalendarMatrix(startOfMonth.toLocalDate()));

        return "main";
    }

    @GetMapping("/main")
    public String redirectToHome(@RequestParam(required = false) String date) {
        return "redirect:/home" + (date != null ? "?date=" + date : "");
    }

    @GetMapping("/main/create")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String createTaskForm(Model model) {
        model.addAttribute("task", new Task());
        model.addAttribute("date", LocalDate.now());
        return "create";
    }

    @GetMapping("/main/create/{date}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String createTaskFormWithDate(@PathVariable String date, Model model) {
        Task task = new Task();
        task.setDate(LocalDate.parse(date).atStartOfDay());
        model.addAttribute("task", task);
        model.addAttribute("date", LocalDate.parse(date));
        return "create";
    }

    @PostMapping("/main/create")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String createTask(@RequestParam("title") String title,
                             @RequestParam("date") String date,
                             @RequestParam("text") String text,
                             @AuthenticationPrincipal UserDetails userDetails) {
        Task task = new Task();
        task.setTitle(title);
        task.setDate(LocalDate.parse(date, DATE_FORMATTER).atStartOfDay());
        task.setText(text);
        task.setName(userDetails.getUsername());
        task.setDone(false);
        taskService.saveTask(task);
        return "redirect:/home";
    }

    @GetMapping("/main/edit/{id}")
    @PreAuthorize("hasRole('ADMIN') or @taskService.getTaskById(#id)?.name == authentication.name")
    public String editTaskForm(@PathVariable int id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Task task = taskService.getTaskById(id);
        model.addAttribute("task", task);
        return "edit";
    }

    @PostMapping("/main/edit/{id}")
    @PreAuthorize("hasRole('ADMIN') or @taskService.getTaskById(#id)?.name == authentication.name")
    public String editTask(@PathVariable int id,
                           @RequestParam("title") String title,
                           @RequestParam("date") String date,
                           @RequestParam("text") String text,
                           @RequestParam(name = "done", required = false) boolean done,
                           @AuthenticationPrincipal UserDetails userDetails) {
        Task existingTask = taskService.getTaskById(id);
        existingTask.setTitle(title);
        existingTask.setDate(LocalDate.parse(date, DATE_FORMATTER).atStartOfDay());
        existingTask.setText(text);
        existingTask.setDone(done);
        taskService.saveTask(existingTask);
        return "redirect:/home";
    }

    @PostMapping("/main/delete/{id}")
    @PreAuthorize("hasRole('ADMIN') or @taskService.getTaskById(#id)?.name == authentication.name")
    public String deleteTask(@PathVariable int id, @AuthenticationPrincipal UserDetails userDetails) {
        taskService.deleteTask(id);
        return "redirect:/home";
    }

    private List<List<LocalDate>> createCalendarMatrix(LocalDate startOfMonth) {
        List<List<LocalDate>> calendarMatrix = new ArrayList<>();
        List<LocalDate> week = new ArrayList<>();

        LocalDate date = startOfMonth;
        int daysToSubtract = date.getDayOfWeek().getValue() % 7;
        LocalDate start = date.minusDays(daysToSubtract);

        for (int i = 0; i < 42; i++) {
            week.add(start);
            start = start.plusDays(1);

            if (week.size() == 7) {
                calendarMatrix.add(new ArrayList<>(week));
                week.clear();
            }
        }

        return calendarMatrix;
    }
}
