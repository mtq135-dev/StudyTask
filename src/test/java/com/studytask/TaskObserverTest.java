package com.studytask;
import com.studytask.model.*;
import com.studytask.observer.TaskObserver;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class TaskObserverTest {
    @Test void successfulChangesNotifyAndFailuresDoNot() {
        TaskModel m=new TaskModel(); AtomicInteger a=new AtomicInteger(),b=new AtomicInteger();
        TaskObserver observer=a::incrementAndGet; m.addObserver(observer); m.addObserver(observer); m.addObserver(b::incrementAndGet);
        m.addTask(TaskTest.task("a")); m.updateTask(TaskTest.task("a").withTitle("Edit"));
        m.updateTaskStatus("a",TaskStatus.COMPLETED); m.updateTaskPriority("a",Priority.LOW);
        assertThrows(IllegalArgumentException.class,()->m.addTask(TaskTest.task("a")));
        assertThrows(IllegalArgumentException.class,()->m.deleteTask("missing"));
        assertEquals(4,a.get()); assertEquals(4,b.get());
        m.removeObserver(observer); m.deleteTask("a"); assertEquals(4,a.get()); assertEquals(5,b.get());
    }
}
