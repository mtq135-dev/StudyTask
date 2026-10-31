package com.studytask;
import com.studytask.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TaskModelTest {
    @Test void subjectsCanBeManagedWithoutLosingTaskIdentity() {
        TaskModel model=new TaskModel();model.addSubject("Math");assertEquals(java.util.List.of("Math"),model.getCourses());
        assertThrows(IllegalArgumentException.class,()->model.addSubject("math"));
        model.addTask(TaskTest.task("a").withCourse("Math"));
        model.renameSubject("Math","Mathematics");
        assertEquals("Mathematics",model.getTaskById("a").orElseThrow().getCourse());
        assertEquals("a",model.getTaskById("a").orElseThrow().getId());
        assertThrows(IllegalArgumentException.class,()->model.deleteSubject("Mathematics"));
        model.deleteTask("a");model.deleteSubject("Mathematics");assertTrue(model.getCourses().isEmpty());
    }
    @Test void fullLifecycleAndFilters() {
        TaskModel m=new TaskModel(); m.addTask(TaskTest.task("a")); m.addTask(TaskTest.task("b").withCourse("Math"));
        assertEquals(2,m.getAllTasks().size()); assertEquals(1,m.filterByCourse("cs101").size());
        m.updateTask(TaskTest.task("a").withTitle("Edited")); assertEquals("Edited",m.getTaskById("a").orElseThrow().getTitle());
        m.updateTaskStatus("a",TaskStatus.COMPLETED); m.updateTaskPriority("a",Priority.HIGH);
        assertEquals(Priority.HIGH,m.getTaskById("a").orElseThrow().getPriority());
        assertEquals(1,m.filterByStatus(TaskStatus.COMPLETED).size());
        m.deleteTask("a"); assertTrue(m.getTaskById("a").isEmpty()); assertEquals(1,m.getAllTasks().size());
    }
    @Test void failuresPreserveStateAndSnapshots() {
        TaskModel m=new TaskModel(); m.addTask(TaskTest.task("a")); var snapshot=m.getAllTasks();
        assertThrows(IllegalArgumentException.class,()->m.addTask(TaskTest.task("a")));
        assertThrows(IllegalArgumentException.class,()->m.updateTask(TaskTest.task("missing")));
        assertThrows(IllegalArgumentException.class,()->m.deleteTask("missing"));
        assertThrows(IllegalArgumentException.class,()->m.updateTaskStatus("missing",TaskStatus.PENDING));
        assertThrows(NullPointerException.class,()->m.updateTaskPriority("a",null));
        assertThrows(UnsupportedOperationException.class,()->snapshot.clear());
        m.addTask(TaskTest.task("b")); assertEquals(1,snapshot.size()); assertEquals(2,m.getAllTasks().size());
    }
}
