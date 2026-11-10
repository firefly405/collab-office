package com.collaboffice.task.service;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.task.entity.TaskTask;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public final class TaskSchedule {
    private TaskSchedule() {}

    public static void validateRule(String rule) {
        if (!Set.of("none","daily","weekly","monthly").contains(rule))
            throw new BusinessException(400,"重复规则无效");
    }

    public static LocalDate nextDate(LocalDate due, String rule, LocalDate today) {
        validateRule(rule);
        LocalDate base=due==null || due.isBefore(today)?today:due;
        return switch(rule) {
            case "daily" -> base.plusDays(1);
            case "weekly" -> base.plusWeeks(1);
            case "monthly" -> base.plusMonths(1);
            default -> null;
        };
    }

    public static boolean shouldRemind(TaskTask task, LocalDateTime now) {
        if(Integer.valueOf(0).equals(task.getReminderEnabled()) || Integer.valueOf(1).equals(task.getIsArchived())
                || task.getDueDate()==null || task.getAssigneeId()==null || task.getStatus()!=null && task.getStatus()>=2) return false;
        if(task.getDueDate().isAfter(now.toLocalDate().plusDays(task.getReminderDays()==null?1:task.getReminderDays()))) return false;
        LocalDateTime last=task.getLastRemindedAt();
        if(last==null) return true;
        return switch(task.getReminderFrequency()==null?"daily":task.getReminderFrequency()) {
            case "once" -> false;
            case "hourly" -> !last.plusHours(1).isAfter(now);
            default -> last.toLocalDate().isBefore(now.toLocalDate());
        };
    }
}
