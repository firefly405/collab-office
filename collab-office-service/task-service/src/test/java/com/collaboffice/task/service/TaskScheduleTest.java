package com.collaboffice.task.service;

import com.collaboffice.task.entity.TaskTask;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class TaskScheduleTest {
    @Test void monthlyScheduleHandlesMonthEndAndOverdueTasks() {
        assertEquals(LocalDate.of(2026,2,28),TaskSchedule.nextDate(LocalDate.of(2026,1,31),"monthly",LocalDate.of(2026,1,1)));
        assertEquals(LocalDate.of(2026,10,16),TaskSchedule.nextDate(LocalDate.of(2026,1,1),"daily",LocalDate.of(2026,10,15)));
        assertNull(TaskSchedule.nextDate(null,"none",LocalDate.now()));
    }
    @Test void remindersHonorDisabledFrequencyAndOverdueStatus() {
        var t=new TaskTask();t.setAssigneeId(1L);t.setDueDate(LocalDate.of(2026,10,14));t.setStatus(0);t.setReminderDays(2);
        var now=LocalDateTime.of(2026,10,15,12,0);
        assertTrue(TaskSchedule.shouldRemind(t,now));
        t.setLastRemindedAt(now.minusHours(2));t.setReminderFrequency("daily");assertFalse(TaskSchedule.shouldRemind(t,now));
        t.setReminderFrequency("hourly");assertTrue(TaskSchedule.shouldRemind(t,now));
        t.setReminderFrequency("once");assertFalse(TaskSchedule.shouldRemind(t,now));
        t.setLastRemindedAt(null);t.setReminderEnabled(0);assertFalse(TaskSchedule.shouldRemind(t,now));
        t.setReminderEnabled(1);t.setStatus(2);assertFalse(TaskSchedule.shouldRemind(t,now));
    }
}
