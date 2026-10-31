package com.studytask;

import com.studytask.controller.TaskController;
import com.studytask.model.*;
import com.studytask.persistence.TaskStorage;
import com.studytask.view.TaskView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.util.List;

/** Shared non-Application entry point avoids Java launcher special handling of JavaFX. */
public final class Main {
    public static void main(String[] args) { Application.launch(Desktop.class,args); }
    public static final class Desktop extends Application {
        private TaskController controller;
        @Override public void start(Stage stage) {
            TaskStorage storage = new TaskStorage(TaskStorage.defaultPath());
            List<Task> tasks=List.of(); String failure=null;
            try { tasks=storage.load(); } catch (Exception e) { failure="Could not load "+storage.path()+". Original data is preserved; saving is disabled.\n"+e.getMessage(); }
            List<String> subjects=List.of();
            try {subjects=storage.loadSubjects();} catch(Exception e) {failure=(failure==null?"":failure+"\n")+"Could not load subjects.json. Original subject data is preserved.\n"+e.getMessage();}
            TaskView view=new TaskView(); controller=new TaskController(new TaskModel(tasks,subjects),view,storage);
            Scene scene=new Scene(view,1280,820); scene.getStylesheets().add(Main.class.getResource("/styles.css").toExternalForm());
            stage.getIcons().add(new Image(Main.class.getResource("/icons/studytask.png").toExternalForm()));
            stage.setTitle("StudyTask"); stage.setMinWidth(1120); stage.setMinHeight(680); stage.setScene(scene); stage.show();
            if(failure!=null) view.error(failure);
        }
    }
}
