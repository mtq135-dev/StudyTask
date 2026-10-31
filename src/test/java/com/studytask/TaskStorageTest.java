package com.studytask;
import com.studytask.persistence.TaskStorage;
import com.studytask.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.List;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
class TaskStorageTest {
    @Test void emptySubjectsPersistAndCorruptSubjectFileIsProtected() throws Exception {
        TaskStorage storage=new TaskStorage(dir.resolve("tasks.json"));storage.load();assertTrue(storage.loadSubjects().isEmpty());
        storage.saveSubjects(List.of("Mathematics","English"));
        assertEquals(List.of("Mathematics","English"),new TaskStorage(storage.path()).loadSubjects());
        Files.writeString(dir.resolve("subjects.json"),"broken");
        assertThrows(IOException.class,storage::loadSubjects);
        assertThrows(IOException.class,()->storage.saveSubjects(List.of()));assertEquals("broken",Files.readString(dir.resolve("subjects.json")));
    }
    @TempDir Path dir;
    @Test void roundTrip() throws Exception {
        TaskStorage s=new TaskStorage(dir.resolve("tasks.json")); assertTrue(s.load().isEmpty());
        Task t=TaskTest.task("a").withPriority(Priority.HIGH).withStatus(TaskStatus.COMPLETED); s.save(List.of(t));
        Task loaded=new TaskStorage(s.path()).load().getFirst();
        assertEquals(t,loaded); assertEquals(t.getDeadline(),loaded.getDeadline()); assertEquals(t.getTitle(),loaded.getTitle());
        assertEquals(Priority.HIGH,loaded.getPriority()); assertEquals(TaskStatus.COMPLETED,loaded.getStatus());
    }
    @Test void corruptionPreserved() throws Exception {
        Path file=dir.resolve("tasks.json"); Files.writeString(file,"{broken"); TaskStorage s=new TaskStorage(file);
        assertThrows(IOException.class,s::load); assertThrows(IOException.class,()->s.save(List.of())); assertEquals("{broken",Files.readString(file));
    }
    @Test void inProgressSurvivesRestart() throws Exception {
        TaskStorage storage=new TaskStorage(dir.resolve("tasks.json")); storage.load();
        storage.save(List.of(TaskTest.task("in-progress").withStatus(TaskStatus.IN_PROGRESS)));
        var restored=new TaskStorage(storage.path()).load().getFirst();
        assertEquals("in-progress",restored.getId()); assertEquals(TaskStatus.IN_PROGRESS,restored.getStatus());
    }
    @Test void duplicateIdentifiersRejected() throws Exception {
        TaskStorage s=new TaskStorage(dir.resolve("tasks.json")); s.load(); s.save(List.of(TaskTest.task("a"),TaskTest.task("a")));
        assertThrows(IOException.class,s::load); assertThrows(IOException.class,()->s.save(List.of()));
    }
}
