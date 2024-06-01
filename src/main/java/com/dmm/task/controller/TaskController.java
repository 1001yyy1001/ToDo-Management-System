package com.dmm.task.controller;

import java.time.LocalDate;
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
        List<List<LocalDate>> matrix = createCalendarMatrix(targetDate, model, userDetails);
        model.addAttribute("matrix", matrix);
        return "main";
    }

    private List<List<LocalDate>> createCalendarMatrix(LocalDate startOfMonth, Model model, UserDetails userDetails) {
        List<List<LocalDate>> calendarMatrix = new ArrayList<>();
        List<LocalDate> week = new ArrayList<>();

        LocalDate date = startOfMonth;
        int daysToSubtract = date.getDayOfWeek().getValue() % 7;  // 月の始まりの日付を週の始まり（日曜日）に合わせるために必要な日数
        LocalDate start = date.minusDays(daysToSubtract);  // カレンダー表示の最初の日

        // カレンダー表示の最後の日を計算
        int daysInMonth = startOfMonth.lengthOfMonth();  // その月の日数
        int totalDays = daysToSubtract + daysInMonth;  // 開始前に追加される日数とその月の日数を合計
        int numWeeks = (int) Math.ceil(totalDays / 7.0);  // 表示する週の数

        LocalDate end = start.plusDays(numWeeks * 7 - 1);  // 表示する最後の日（日曜日開始でnumWeeks週間後）

        // タスクの取得
        List<Task> tasks;
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            tasks = taskService.findTasksByDateBetween(start.atStartOfDay(), end.atTime(LocalTime.MAX));
        } else {
            tasks = taskService.findTasksByDateBetween(start.atStartOfDay(), end.atTime(LocalTime.MAX), userDetails.getUsername());
        }
        Map<LocalDate, List<Task>> tasksByDate = tasks.stream()
            .collect(Collectors.groupingBy(task -> task.getDate().toLocalDate()));
        model.addAttribute("tasks", tasksByDate);

        // カレンダー行を生成
        for (int i = 0; i < numWeeks * 7; i++) {  // numWeeks週間分の日付を生成
            week.add(start.plusDays(i));
            if (week.size() == 7) {
                calendarMatrix.add(new ArrayList<>(week));
                week.clear();
            }
        }

        // 表示用に月と年をモデルに追加
        model.addAttribute("month", startOfMonth.format(YEAR_MONTH_FORMATTER));
        model.addAttribute("prev", startOfMonth.minusMonths(1));
        model.addAttribute("next", startOfMonth.plusMonths(1));
        
        return calendarMatrix;
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
}
