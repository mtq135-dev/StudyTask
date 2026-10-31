package com.studytask.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.Objects;

/**
 * RI: id, title and course are nonblank; deadline, priority, description and status are nonnull.
 * AF: this represents the study task identified by id, with the corresponding academic details.
 * All fields are immutable values; no mutable representation escapes.
 */
public final class Task {
    private final String id, title, course, description;
    private final LocalDate deadline;
    private final Priority priority;
    private final TaskStatus status;

    @JsonCreator
    public Task(@JsonProperty("id") String id, @JsonProperty("title") String title,
                @JsonProperty("course") String course, @JsonProperty("deadline") LocalDate deadline,
                @JsonProperty("priority") Priority priority, @JsonProperty("description") String description,
                @JsonProperty("status") TaskStatus status) {
        this.id = required(id, "Identifier"); this.title = required(title, "Title");
        this.course = required(course, "Course"); this.deadline = Objects.requireNonNull(deadline, "Deadline is required");
        this.priority = Objects.requireNonNull(priority, "Priority is required");
        this.description = Objects.requireNonNull(description, "Description is required");
        this.status = Objects.requireNonNull(status, "Status is required");
    }
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.strip();
    }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCourse() { return course; }
    public String getDescription() { return description; }
    public LocalDate getDeadline() { return deadline; }
    public Priority getPriority() { return priority; }
    public TaskStatus getStatus() { return status; }
    public Task withTitle(String v) { return new Task(id,v,course,deadline,priority,description,status); }
    public Task withCourse(String v) { return new Task(id,title,v,deadline,priority,description,status); }
    public Task withDeadline(LocalDate v) { return new Task(id,title,course,v,priority,description,status); }
    public Task withDescription(String v) { return new Task(id,title,course,deadline,priority,v,status); }
    public Task withPriority(Priority v) { return new Task(id,title,course,deadline,v,description,status); }
    public Task withStatus(TaskStatus v) { return new Task(id,title,course,deadline,priority,description,v); }
    @Override public boolean equals(Object other) { return other instanceof Task t && id.equals(t.id); }
    @Override public int hashCode() { return id.hashCode(); }
}
