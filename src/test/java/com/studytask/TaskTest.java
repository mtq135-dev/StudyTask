package com.studytask;
import com.studytask.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;
class TaskTest {
    static Task task(String id) { return new Task(id,"Read chapter","CS101",LocalDate.of(2026,11,1),Priority.MEDIUM,"Notes",TaskStatus.PENDING); }
    @Test void equalityContract() {
        Task a=task("a"), b=a.withTitle("Other"), c=b.withPriority(Priority.HIGH);
        assertEquals(a,a); assertEquals(a,b); assertEquals(b,a); assertEquals(b,c); assertEquals(a,c);
        assertEquals(a.hashCode(),c.hashCode()); assertNotEquals(a,task("b")); assertNotEquals(a,null); assertNotEquals(a,"a");
        assertEquals(1,new HashSet<>(java.util.List.of(a,b,c)).size());
    }
    @Test void updatesAreImmutable() {
        Task a=task("a");
        assertEquals("Read chapter",a.getTitle()); assertEquals("Other",a.withTitle("Other").getTitle());
        assertEquals("New",a.withCourse("New").getCourse()); assertEquals("Text",a.withDescription("Text").getDescription());
        assertEquals(LocalDate.MAX,a.withDeadline(LocalDate.MAX).getDeadline());
        assertEquals(TaskStatus.COMPLETED,a.withStatus(TaskStatus.COMPLETED).getStatus());
        assertEquals(Priority.HIGH,a.withPriority(Priority.HIGH).getPriority());
        assertEquals(TaskStatus.PENDING,a.getStatus()); assertEquals(Priority.MEDIUM,a.getPriority());
        assertNotSame(a,a.withTitle(a.getTitle())); assertEquals("a",a.withTitle("Other").getId());
    }
    @Test void invalidValuesRejected() {
        Task a=task("a");
        assertThrows(IllegalArgumentException.class,()->task(" "));
        assertThrows(IllegalArgumentException.class,()->a.withTitle(" "));
        assertThrows(IllegalArgumentException.class,()->a.withTitle(null));
        assertThrows(IllegalArgumentException.class,()->a.withCourse(""));
        assertThrows(NullPointerException.class,()->a.withDeadline(null));
        assertThrows(NullPointerException.class,()->a.withPriority(null));
        assertThrows(NullPointerException.class,()->a.withStatus(null));
        assertThrows(NullPointerException.class,()->a.withDescription(null));
    }
}
