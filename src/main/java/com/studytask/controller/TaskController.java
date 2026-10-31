package com.studytask.controller;

import com.studytask.model.*;
import com.studytask.observer.TaskObserver;
import com.studytask.persistence.TaskStorage;
import com.studytask.view.TaskView;
import javafx.application.Platform;
import java.io.IOException;

public final class TaskController implements TaskObserver {
    private final TaskModel model;
    private final TaskView view;
    private final TaskStorage storage;
    public TaskController(TaskModel model, TaskView view, TaskStorage storage) {
        this.model=model; this.view=view; this.storage=storage; model.addObserver(this);
        view.setSubjectActions(name -> act(() -> model.addSubject(name)),(current,replacement) -> act(() -> model.renameSubject(current,replacement)),name -> act(() -> model.deleteSubject(name)));
        view.add.setOnAction(e -> view.taskDialog(null).ifPresent(t -> act(() -> model.addTask(t))));
        view.edit.setOnAction(e -> view.taskDialog(view.selected()).ifPresent(t -> act(() -> model.updateTask(t))));
        view.delete.setOnAction(e -> { Task t=view.selected(); if(t!=null && view.confirmDelete(t)) act(() -> model.deleteTask(t.getId())); });
        view.toggle.setOnAction(e -> { Task t=view.selected(); act(() -> model.updateTaskStatus(t.getId(),t.getStatus()==TaskStatus.COMPLETED?TaskStatus.PENDING:TaskStatus.COMPLETED)); });
        view.changePriority.setOnAction(e -> act(() -> model.updateTaskPriority(view.selected().getId(),view.priority.getValue())));
        view.status.valueProperty().addListener((o,oldValue,newValue) -> refresh());
        view.course.valueProperty().addListener((o,oldValue,newValue) -> refresh());
        view.search.textProperty().addListener((o,oldValue,newValue) -> refresh());
        view.reset.setOnAction(e -> { view.status.setValue("All statuses"); view.course.setValue("All courses"); view.search.clear(); refresh(); });
        refresh();
    }
    private void act(Runnable action) { try { action.run(); } catch (RuntimeException ex) { view.error(ex.getMessage()); } }
    private void refresh() {
        if (view.isRendering()) return;
        var all=model.getAllTasks();
        String selectedCourse=view.course.getValue();
        if (selectedCourse!=null && !selectedCourse.equals("All courses") && !model.getCourses().contains(selectedCourse)) view.course.setValue("All courses");
        String query=view.search.getText().strip().toLowerCase(java.util.Locale.ROOT);
        var filtered=all.stream().filter(t -> view.status.getValue()==null || view.status.getValue().equals("All statuses")
                        || t.getStatus().name().equals(view.status.getValue())
                        || view.status.getValue().equals("OVERDUE") && t.getStatus()!=TaskStatus.COMPLETED && t.getDeadline().isBefore(java.time.LocalDate.now()))
                .filter(t -> view.course.getValue()==null || view.course.getValue().equals("All courses") || t.getCourse().equals(view.course.getValue()))
                .filter(t -> (t.getTitle()+" "+t.getCourse()+" "+t.getDescription()).toLowerCase(java.util.Locale.ROOT).contains(query)).toList();
        view.render(all,filtered,model.getCourses());
    }
    @Override public void onTasksChanged() {
        if (!Platform.isFxApplicationThread()) { Platform.runLater(this::onTasksChanged); return; }
        refresh();
        try { storage.save(model.getAllTasks()); storage.saveSubjects(model.getCourses()); }
        catch (IOException ex) { view.error("Changes remain in memory but could not be saved to " +storage.path()+".\n"+ex.getMessage()); }
    }
}
