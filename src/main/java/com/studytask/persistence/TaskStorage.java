package com.studytask.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.studytask.model.Task;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Atomic JSON storage. A failed load never authorizes overwriting the source file. */
public final class TaskStorage {
    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private boolean writable;
    private boolean subjectsWritable;
    public TaskStorage(Path file) { this.file = file; }
    public static Path defaultPath() {
        String local = System.getenv("LOCALAPPDATA");
        return (local == null || local.isBlank() ? Path.of(System.getProperty("user.home"), ".local", "share") : Path.of(local))
                .resolve("StudyTask").resolve("tasks.json");
    }
    public Path path() { return file; }
    public List<String> loadSubjects() throws IOException {
        subjectsWritable=false;Path subjects=file.resolveSibling("subjects.json");
        if(!Files.exists(subjects)) {subjectsWritable=true;return List.of();}
        List<String> names=mapper.readValue(subjects.toFile(),new TypeReference<List<String>>() {});
        if(names==null || names.stream().anyMatch(s -> s==null || s.isBlank())) throw new IOException("Invalid subject names");
        subjectsWritable=true;return names.stream().map(String::strip).distinct().toList();
    }
    public void saveSubjects(List<String> names) throws IOException {
        if(!subjectsWritable) throw new IOException("Subject storage is locked after a failed load; recover subjects.json and restart");
        writeAtomically(file.resolveSibling("subjects.json"),names);
    }
    public List<Task> load() throws IOException {
        writable = false;
        if (!Files.exists(file)) { writable = true; return List.of(); }
        List<Task> loaded = mapper.readValue(file.toFile(), new TypeReference<List<Task>>() {});
        if (loaded == null) throw new IOException("Task file must contain an array");
        Set<String> ids = new HashSet<>();
        for (Task t : loaded) if (t == null || !ids.add(t.getId())) throw new IOException("Invalid or duplicate task");
        writable = true; return List.copyOf(loaded);
    }
    public void save(List<Task> tasks) throws IOException {
        if (!writable) throw new IOException("Storage is locked after a failed load; recover the original file and restart");
        writeAtomically(file,tasks);
    }
    private void writeAtomically(Path destination,Object value) throws IOException {
        Files.createDirectories(destination.toAbsolutePath().getParent());
        Path temp = Files.createTempFile(destination.toAbsolutePath().getParent(), "studytask-", ".tmp");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(),value);
            try { Files.move(temp,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp,destination,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
