package com.studytask.model;

import com.studytask.observer.TaskObserver;
import java.util.*;

/** JavaFX-independent authoritative task collection. Used on one application thread. */
public final class TaskModel {
    private final Map<String,Task> tasks = new LinkedHashMap<>();
    private final Set<TaskObserver> observers = new LinkedHashSet<>();
    private final Set<String> subjects = new LinkedHashSet<>();
    public TaskModel() { }
    public TaskModel(Collection<Task> initial) {
        for (Task t : initial) {
            Objects.requireNonNull(t);
            if (tasks.putIfAbsent(t.getId(),t) != null) throw new IllegalArgumentException("Duplicate identifier");
            subjects.add(t.getCourse());
        }
    }
    public TaskModel(Collection<Task> initial,Collection<String> savedSubjects) {
        this(initial);
        for(String subject:savedSubjects) subjects.add(subjectName(subject));
    }
    public List<String> getCourses() {
        Set<String> names=new TreeSet<>(subjects); tasks.values().forEach(t -> names.add(t.getCourse())); return List.copyOf(names);
    }
    private String subjectName(String name) {
        if(name==null || name.isBlank()) throw new IllegalArgumentException("Enter a subject name.");return name.strip();
    }
    public void addSubject(String name) {
        String valid=subjectName(name);
        if(getCourses().stream().anyMatch(c -> c.equalsIgnoreCase(valid))) throw new IllegalArgumentException("That subject already exists.");
        subjects.add(valid);notifyObservers();
    }
    public void renameSubject(String current,String replacement) {
        String valid=subjectName(replacement);
        if(!getCourses().contains(current)) throw new IllegalArgumentException("Subject no longer exists.");
        if(getCourses().stream().anyMatch(c -> !c.equals(current) && c.equalsIgnoreCase(valid))) throw new IllegalArgumentException("That subject already exists.");
        tasks.replaceAll((id,t) -> t.getCourse().equals(current)?t.withCourse(valid):t);
        subjects.remove(current);subjects.add(valid);notifyObservers();
    }
    public void deleteSubject(String name) {
        if(!getCourses().contains(name)) throw new IllegalArgumentException("Subject no longer exists.");
        if(tasks.values().stream().anyMatch(t -> t.getCourse().equals(name))) throw new IllegalArgumentException("Move or delete this subject's tasks before deleting the subject.");
        subjects.remove(name);notifyObservers();
    }
    public void addObserver(TaskObserver o) { observers.add(Objects.requireNonNull(o)); }
    public void removeObserver(TaskObserver o) { observers.remove(o); }
    private void notifyObservers() { for (TaskObserver o : List.copyOf(observers)) o.onTasksChanged(); }
    public List<Task> getAllTasks() { return List.copyOf(tasks.values()); }
    public Optional<Task> getTaskById(String id) { return Optional.ofNullable(tasks.get(id)); }
    private Task require(String id) { return getTaskById(id).orElseThrow(() -> new IllegalArgumentException("Task no longer exists")); }
    public void addTask(Task task) {
        Objects.requireNonNull(task);
        if (tasks.putIfAbsent(task.getId(),task) != null) throw new IllegalArgumentException("Duplicate identifier");
        subjects.add(task.getCourse());
        notifyObservers();
    }
    public void updateTask(Task task) { Objects.requireNonNull(task); require(task.getId()); tasks.put(task.getId(),task); subjects.add(task.getCourse()); notifyObservers(); }
    public void deleteTask(String id) { require(id); tasks.remove(id); notifyObservers(); }
    public void updateTaskStatus(String id, TaskStatus status) { updateTask(require(id).withStatus(status)); }
    public void updateTaskPriority(String id, Priority priority) { updateTask(require(id).withPriority(priority)); }
    public List<Task> filterByStatus(TaskStatus status) { return tasks.values().stream().filter(t -> t.getStatus()==Objects.requireNonNull(status)).toList(); }
    public List<Task> filterByCourse(String course) { Objects.requireNonNull(course); return tasks.values().stream().filter(t -> t.getCourse().equalsIgnoreCase(course)).toList(); }
}
