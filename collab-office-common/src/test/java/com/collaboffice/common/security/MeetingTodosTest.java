package com.collaboffice.common.security;
import com.collaboffice.common.util.MeetingTodos;
import com.collaboffice.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class MeetingTodosTest {
 @Test void keepsLinkedTasksAndRejectsForgedOrRemovedLinks(){var old=Map.of("id","a","title","Review","taskId","12","spaceId",13L);assertEquals("12",MeetingTodos.normalize(List.of(old),List.of(old),13L).get(0).get("taskId"));assertThrows(BusinessException.class,()->MeetingTodos.normalize(List.of(),List.of(old),13L));assertThrows(BusinessException.class,()->MeetingTodos.normalize(List.of(old),List.of(),13L));}
 @Test void rejectsDuplicateAndBadDates(){var item=Map.of("id","a","title","Review");assertThrows(BusinessException.class,()->MeetingTodos.normalize(List.of(item,item),List.of(),13L));assertThrows(BusinessException.class,()->MeetingTodos.normalize(List.of(Map.of("id","a","title","Review","dueDate","tomorrow")),List.of(),13L));}
}
