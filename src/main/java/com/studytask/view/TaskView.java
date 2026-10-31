package com.studytask.view;

import com.studytask.model.*;
import com.studytask.model.Priority;
import com.studytask.persistence.TaskStorage;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.prefs.Preferences;

/** Presentation snapshots and navigation; task mutations are handled by the controller. */
public final class TaskView extends BorderPane {
    public final Button add=new Button("Add Task"), edit=new Button("Edit task"), delete=new Button("Delete"),
            toggle=new Button("Complete / To do"), reset=new Button("Reset"), changePriority=new Button("Set priority");
    public final ComboBox<String> course=new ComboBox<>(), status=new ComboBox<>();
    public final ComboBox<Priority> priority=new ComboBox<>(FXCollections.observableArrayList(Priority.values()));
    public final TextField search=new TextField();
    public final TableView<Task> table=new TableView<>();
    private final Map<String,Button> navigation=new LinkedHashMap<>();
    private final Map<String,ToggleButton> tabs=new LinkedHashMap<>();
    private final Preferences preferences=Preferences.userNodeForPackage(TaskView.class);
    private final VBox workspace=new VBox(24), tasksPage=new VBox(20);
    private final Label pageTitle=label("Home","title"), subtitle=label("","muted");
    private List<Task> snapshot=List.of();
    private List<String> subjectSnapshot=List.of();
    private Consumer<String> addSubjectAction,deleteSubjectAction;
    private BiConsumer<String,String> renameSubjectAction;
    private String page="Home", settingsSection="General";
    private YearMonth month=YearMonth.now();
    private boolean rendering;
    private final DateTimeFormatter shortDate=DateTimeFormatter.ofPattern("MMM d, yyyy",Locale.ENGLISH);
    private static final String[] COLORS={"#2f6bff","#9460ff","#ffb347","#22bf94","#fb789c","#36b6dc"};

    public TaskView() {
        getStyleClass().add("app-root"); applyAppearance(); setLeft(sidebar());
        workspace.getStyleClass().add("workspace"); workspace.setPadding(new Insets(32)); setCenter(workspace);
        status.getItems().setAll("All statuses","PENDING","IN_PROGRESS","COMPLETED","OVERDUE"); status.setValue("All statuses");
        course.getItems().setAll("All courses"); course.setValue("All courses"); course.setPrefWidth(150);
        course.setAccessibleText("Course filter"); status.setAccessibleText("Task status");
        search.setPromptText("Search tasks…"); search.setPrefWidth(185); search.setAccessibleText("Search tasks");
        add.setGraphic(icon("plus")); add.getStyleClass().add("primary"); setupTable();
        HBox tabRow=new HBox(6); tabRow.setAlignment(Pos.CENTER_LEFT); ToggleGroup group=new ToggleGroup();
        String[][] options={{"All","All statuses"},{"To Do","PENDING"},{"In Progress","IN_PROGRESS"},{"Completed","COMPLETED"},{"Overdue","OVERDUE"}};
        for(String[] option:options) {
            ToggleButton b=new ToggleButton(option[0]); b.getStyleClass().add("filter-tab"); b.setToggleGroup(group);
            b.setSelected(option[1].equals(status.getValue())); b.setOnAction(e -> {status.setValue(option[1]);b.setSelected(true);});
            tabs.put(option[1],b); tabRow.getChildren().add(b);
        }
        status.valueProperty().addListener((o,a,b) -> {if(tabs.containsKey(b)) tabs.get(b).setSelected(true);});
        reset.getStyleClass().add("quiet");
        HBox filters=new HBox(10,tabRow,spacer(),search,course,reset); filters.setAlignment(Pos.CENTER_LEFT);
        priority.setValue(Priority.MEDIUM); priority.setPrefWidth(115); friendlyCombo(priority);
        for(Button b:List.of(edit,delete,toggle,changePriority)) {
            b.getStyleClass().add("secondary"); b.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        }
        delete.getStyleClass().add("danger");
        HBox actions=new HBox(10,edit,toggle,spacer(),priority,changePriority,delete); actions.setAlignment(Pos.CENTER_LEFT);
        VBox taskCard=new VBox(table); taskCard.getStyleClass().add("table-card"); VBox.setVgrow(table,javafx.scene.layout.Priority.ALWAYS);
        VBox.setVgrow(taskCard,javafx.scene.layout.Priority.ALWAYS);
        tasksPage.getChildren().addAll(filters,taskCard,actions,label("Select a task to use the actions above. Double-click a row to edit its details.","muted"));
        showPage("Home");
    }

    private VBox sidebar() {
        HBox brand=new HBox(9,logo(32),label("StudyTask","brand")); brand.setAlignment(Pos.CENTER_LEFT);
        VBox nav=new VBox(8); nav.setPadding(new Insets(36,0,0,0));
        for(String name:List.of("Home","Tasks","Calendar","Subjects","Analytics","Settings")) {
            Button b=new Button(name,icon(name.toLowerCase(Locale.ROOT))); b.getStyleClass().add("nav-button");
            b.setMaxWidth(Double.MAX_VALUE); b.setOnAction(e -> showPage(name)); navigation.put(name,b); nav.getChildren().add(b);
        }
        Region space=new Region(); VBox.setVgrow(space,javafx.scene.layout.Priority.ALWAYS);
        VBox result=new VBox(brand,nav,space,label("YOUR STUDY, ORGANIZED","sidebar-caption"),label("Local workspace  ·  v1.0.0","sidebar-footnote"));
        result.getStyleClass().add("sidebar"); result.setPrefWidth(206); result.setMinWidth(190); result.setPadding(new Insets(28,16,24,16)); return result;
    }
    private void setupTable() {
        TableColumn<Task,Task> taskColumn=new TableColumn<>("Task");taskColumn.setPrefWidth(350);
        taskColumn.setCellValueFactory(value -> new ReadOnlyObjectWrapper<>(value.getValue()));
        taskColumn.setComparator(Comparator.comparing(Task::getTitle,String.CASE_INSENSITIVE_ORDER));
        taskColumn.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Task t,boolean empty) {
                super.updateItem(t,empty); setText(null); setGraphic(null);
                if(!empty && t!=null) {
                    Label title=label(t.getTitle(),"task-title");
                    if(t.getStatus()==TaskStatus.COMPLETED) title.getStyleClass().add("completed-title");
                    setGraphic(new VBox(5,title,label(t.getCourse(),"small muted")));
                }
            }
        });
        table.getColumns().add(taskColumn);
        column("Priority",t -> pretty(t.getPriority().name()),120).setCellFactory(c -> badgeCell());
        column("Status",this::displayStatus,140).setCellFactory(c -> badgeCell());
        column("Due date",t -> t.getDeadline().format(shortDate),160);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(76); table.getStyleClass().add("tasks-table");
        VBox empty=new VBox(12,icon("tasks"),label("A little clarity starts here","section-title"),label("Add a study task, or adjust your filters.","muted"));
        empty.setAlignment(Pos.CENTER); table.setPlaceholder(empty);
        table.setRowFactory(t -> {TableRow<Task> row=new TableRow<>(); row.setOnMouseClicked(e -> {if(e.getClickCount()==2 && !row.isEmpty()) edit.fire();});return row;});
        table.setAccessibleText("Study tasks. Select a row to use task actions; double-click to edit.");
    }
    private TableCell<Task,String> badgeCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String text,boolean empty) {
                super.updateItem(text,empty);setText(null);setGraphic(null);
                if(!empty && text!=null) {
                    String color=switch(text) {case "Completed","Low" -> "green";case "Overdue","High" -> "red";case "Medium","In Progress" -> "amber";default -> "blue";};
                    setGraphic(label(text,"badge "+color));
                }
            }
        };
    }
    private TableColumn<Task,String> column(String name,Function<Task,String> value,int width) {
        TableColumn<Task,String> c=new TableColumn<>(name); c.setPrefWidth(width); c.setCellValueFactory(d -> new ReadOnlyStringWrapper(value.apply(d.getValue()))); table.getColumns().add(c);return c;
    }
    private String displayStatus(Task t) {return t.getStatus()==TaskStatus.COMPLETED?"Completed":overdue(t)?"Overdue":t.getStatus()==TaskStatus.IN_PROGRESS?"In Progress":"To Do";}
    private boolean overdue(Task t) {return t.getStatus()!=TaskStatus.COMPLETED && t.getDeadline().isBefore(LocalDate.now());}
    public Task selected() {return table.getSelectionModel().getSelectedItem();}
    public boolean isRendering() {return rendering;}
    public void setSubjectActions(Consumer<String> add, BiConsumer<String,String> rename, Consumer<String> delete) {
        addSubjectAction=add;renameSubjectAction=rename;deleteSubjectAction=delete;
    }
    public void render(List<Task> all,List<Task> filtered,List<String> subjects) {
        snapshot=List.copyOf(all);subjectSnapshot=List.copyOf(subjects);String selectedId=selected()==null?null:selected().getId(); rendering=true;
        try {
            String selectedCourse=course.getValue();List<String> courses=new ArrayList<>(List.of("All courses"));
            courses.addAll(subjects);
            if(!course.getItems().equals(courses)) {course.getItems().setAll(courses);course.setValue(courses.contains(selectedCourse)?selectedCourse:"All courses");}
            table.getItems().setAll(filtered);
            if(selectedId!=null) filtered.stream().filter(t -> t.getId().equals(selectedId)).findFirst().ifPresent(t -> table.getSelectionModel().select(t));
        } finally {rendering=false;}
        if(!page.equals("Tasks")) showPage(page);else subtitle.setText("Manage your study tasks  ·  "+filtered.size()+" of "+all.size()+" shown");
    }

    private void showPage(String name) {
        page=name;navigation.forEach((key,b) -> {b.getStyleClass().remove("nav-selected");if(key.equals(name)) b.getStyleClass().add("nav-selected");});
        workspace.getChildren().clear();pageTitle.setText(name);
        subtitle.setText(switch(name) {case "Home" -> "Here's your study overview for today.";case "Tasks" -> "Manage your study tasks";case "Calendar" -> "Your coursework, one deadline at a time";case "Subjects" -> "A clear view of every course";case "Analytics" -> "Understand your workload and progress";default -> "Make this workspace yours";});
        HBox heading=new HBox(16,new VBox(7,pageTitle,subtitle),spacer());heading.setAlignment(Pos.CENTER_LEFT);
        if(name.equals("Tasks")) heading.getChildren().add(add);
        if(name.equals("Subjects")) {Button create=new Button("Add Subject",icon("plus"));create.getStyleClass().add("primary");create.setOnAction(e -> subjectDialog(null).ifPresent(addSubjectAction));heading.getChildren().add(create);}
        if(name.equals("Home")) {
            String user=System.getProperty("user.name","Student");user=user.isEmpty()?"Student":Character.toUpperCase(user.charAt(0))+user.substring(1);
            int hour=LocalTime.now().getHour();pageTitle.setText("Good "+(hour<12?"morning":hour<18?"afternoon":"evening")+", "+user+"!");
            heading.getChildren().add(label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy",Locale.ENGLISH)),"date-chip"));
        }
        Node body=switch(name) {case "Home" -> home();case "Tasks" -> tasksPage;case "Calendar" -> calendar();case "Subjects" -> subjects();case "Analytics" -> analytics();default -> settings();};
        VBox.setVgrow(body,javafx.scene.layout.Priority.ALWAYS);workspace.getChildren().addAll(heading,body);
    }
    private Node home() {
        long done=snapshot.stream().filter(t -> t.getStatus()==TaskStatus.COMPLETED).count();
        long today=snapshot.stream().filter(t -> t.getDeadline().equals(LocalDate.now()) && t.getStatus()!=TaskStatus.COMPLETED).count();
        HBox stats=new HBox(16,stat(today,"Due today","blue","calendar"),stat(snapshot.stream().filter(t -> t.getStatus()==TaskStatus.IN_PROGRESS).count(),"In progress","amber","tasks"),stat(done,"Completed","green","check"),stat(snapshot.stream().filter(this::overdue).count(),"Overdue","red","clock"));
        List<Task> focus=snapshot.stream().filter(t -> t.getStatus()!=TaskStatus.COMPLETED && !t.getDeadline().isAfter(LocalDate.now())).sorted(Comparator.comparing(Task::getDeadline)).toList();
        List<Task> upcoming=snapshot.stream().filter(t -> t.getStatus()!=TaskStatus.COMPLETED && t.getDeadline().isAfter(LocalDate.now())).sorted(Comparator.comparing(Task::getDeadline)).limit(3).toList();
        VBox todayCard=taskList("Today's focus",focus,"You're all caught up. A good time to plan ahead.");todayCard.setPrefWidth(500);
        VBox progressLegend=new VBox(18,legend("Completed",done,"green"),legend("Unfinished",snapshot.size()-done,"blue"));
        HBox progressBody=new HBox(16,progressRing(done,snapshot.size()),progressLegend);progressBody.setAlignment(Pos.CENTER_LEFT);HBox.setHgrow(progressLegend,javafx.scene.layout.Priority.ALWAYS);
        VBox right=new VBox(18,card("Your progress",progressBody),taskList("Coming up",upcoming,"No upcoming deadlines yet."));right.setPrefWidth(390);
        HBox content=new HBox(18,todayCard,right);HBox.setHgrow(todayCard,javafx.scene.layout.Priority.ALWAYS);HBox.setHgrow(right,javafx.scene.layout.Priority.ALWAYS);return scroll(new VBox(22,stats,content));
    }
    private VBox stat(long count,String caption,String color,String symbol) {
        HBox top=new HBox(label(Long.toString(count),"stat-value "+color),spacer(),icon(symbol));top.setAlignment(Pos.CENTER_LEFT);
        VBox card=new VBox(18,top,label(caption,"muted"));card.getStyleClass().add("card");card.setMaxWidth(Double.MAX_VALUE);HBox.setHgrow(card,javafx.scene.layout.Priority.ALWAYS);return card;
    }
    private VBox taskList(String title,List<Task> tasks,String empty) {
        Button viewAll=new Button("View all");viewAll.getStyleClass().add("link-button");viewAll.setOnAction(e -> {clearFilters();showPage("Tasks");});
        HBox top=new HBox(label(title,"section-title"),spacer(),viewAll);top.setAlignment(Pos.CENTER_LEFT);VBox box=new VBox(12,top);box.getStyleClass().add("card");
        if(tasks.isEmpty()) {Label message=label(empty,"muted");message.setWrapText(true);box.getChildren().add(message);}
        for(Task t:tasks.stream().limit(5).toList()) {
            Button row=new Button();row.getStyleClass().add("task-link");row.setMaxWidth(Double.MAX_VALUE);
            HBox graphic=new HBox(12,icon("tasks"),new VBox(5,label(t.getTitle(),"task-title"),label(t.getCourse(),"small muted")),spacer(),label(t.getDeadline().equals(LocalDate.now())?"Today":t.getDeadline().format(shortDate),"small muted"));graphic.setAlignment(Pos.CENTER_LEFT);
            row.setGraphic(graphic);row.setOnAction(e -> selectTask(t));box.getChildren().add(row);
        }return box;
    }
    private void clearFilters() {status.setValue("All statuses");course.setValue("All courses");search.clear();}
    private void selectTask(Task t) {clearFilters();showPage("Tasks");table.getSelectionModel().select(t);table.scrollTo(t);}
    private Node progressRing(long done,long total) {
        PieChart chart=new PieChart(FXCollections.observableArrayList(new PieChart.Data("Completed",done),new PieChart.Data("Remaining",Math.max(total-done,total==0?1:0))));
        chart.setLegendVisible(false);chart.setLabelsVisible(false);chart.setAnimated(false);chart.setMinSize(140,140);chart.setPrefSize(140,140);chart.setMaxSize(140,140);chart.getStyleClass().add("progress-chart");
        VBox center=new VBox(4,label((total==0?0:Math.round(100.0*done/total))+"%","ring-value"),label("complete","small muted"));center.setAlignment(Pos.CENTER);center.getStyleClass().add("ring-center");center.setMaxSize(84,84);center.setMouseTransparent(true);return new StackPane(chart,center);
    }
    private HBox legend(String name,long count,String color) {HBox row=new HBox(8,label("●",color+"-text"),label(name,"muted"),spacer(),label(Long.toString(count),"task-title"));row.setAlignment(Pos.CENTER_LEFT);return row;}

    private Node calendar() {
        Button previous=new Button("‹"),next=new Button("›"),today=new Button("Today");for(Button b:List.of(previous,next,today)) b.getStyleClass().add("secondary");
        previous.setAccessibleText("Previous month");next.setAccessibleText("Next month");
        previous.setOnAction(e -> {month=month.minusMonths(1);showPage("Calendar");});next.setOnAction(e -> {month=month.plusMonths(1);showPage("Calendar");});today.setOnAction(e -> {month=YearMonth.now();showPage("Calendar");});
        HBox toolbar=new HBox(12,today,previous,next,label(month.format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.ENGLISH)),"section-title"),spacer(),label("MONTH VIEW","eyebrow"));toolbar.setAlignment(Pos.CENTER_LEFT);
        GridPane grid=new GridPane();grid.getStyleClass().add("calendar-grid");
        for(int c=0;c<7;c++) {ColumnConstraints cc=new ColumnConstraints();cc.setPercentWidth(100.0/7);grid.getColumnConstraints().add(cc);Label day=label(List.of("Mon","Tue","Wed","Thu","Fri","Sat","Sun").get(c),"calendar-heading");day.setMaxWidth(Double.MAX_VALUE);grid.add(day,c,0);}
        LocalDate start=month.atDay(1).minusDays(month.atDay(1).getDayOfWeek().getValue()-1);
        for(int i=0;i<42;i++) {
            LocalDate date=start.plusDays(i);VBox cell=new VBox(6);cell.getStyleClass().add("calendar-day");cell.setMinHeight(100);cell.setPrefHeight(106);
            if(!YearMonth.from(date).equals(month)) cell.getStyleClass().add("outside-month");if(date.equals(LocalDate.now())) cell.getStyleClass().add("today");
            cell.getChildren().add(label(Integer.toString(date.getDayOfMonth()),"small"));List<Task> entries=snapshot.stream().filter(t -> t.getDeadline().equals(date)).toList();
            for(Task t:entries.stream().limit(2).toList()) {Button chip=new Button(t.getTitle());chip.getStyleClass().addAll("calendar-task",t.getStatus()==TaskStatus.COMPLETED?"green":overdue(t)?"red":"blue");chip.setMaxWidth(Double.MAX_VALUE);chip.setTooltip(new Tooltip(t.getTitle()+"\n"+t.getCourse()));chip.setOnAction(e -> selectTask(t));cell.getChildren().add(chip);}
            if(entries.size()>2) {Button more=new Button("+ "+(entries.size()-2)+" more");more.getStyleClass().add("link-button");more.setOnAction(e -> dateTasks(date,entries));cell.getChildren().add(more);}grid.add(cell,i%7,i/7+1);
        }return scroll(new VBox(22,toolbar,grid));
    }
    private void dateTasks(LocalDate date,List<Task> entries) {
        Dialog<Void> d=new Dialog<>();d.setTitle(date.format(shortDate));d.initOwner(getScene().getWindow());VBox list=new VBox(10);
        for(Task t:entries) {Button b=new Button(t.getTitle()+" · "+t.getCourse());b.getStyleClass().add("secondary");b.setOnAction(e -> {d.close();selectTask(t);});list.getChildren().add(b);}
        d.getDialogPane().setContent(list);d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);styleDialog(d);d.showAndWait();
    }
    private Node subjects() {
        TilePane tiles=new TilePane(18,18);tiles.setPrefColumns(3);tiles.setPrefTileWidth(260);List<String> courses=subjectSnapshot;int index=0;
        for(String subject:courses) {
            List<Task> tasks=snapshot.stream().filter(t -> t.getCourse().equals(subject)).toList();long done=tasks.stream().filter(t -> t.getStatus()==TaskStatus.COMPLETED).count();String color=COLORS[index++%COLORS.length];
            Label symbol=label("▤","subject-symbol");symbol.setStyle("-fx-background-color: "+color+";");
            ProgressBar progress=new ProgressBar(tasks.isEmpty()?0:(double)done/tasks.size());progress.setMaxWidth(Double.MAX_VALUE);progress.setStyle("-fx-accent: "+color+";");
            HBox progressRow=new HBox(12,progress,label((tasks.isEmpty()?0:Math.round(100.0*done/tasks.size()))+"%","small muted"));HBox.setHgrow(progress,javafx.scene.layout.Priority.ALWAYS);
            MenuItem rename=new MenuItem("Rename subject"),remove=new MenuItem("Delete empty subject");
            rename.setOnAction(e -> subjectDialog(subject).ifPresent(replacement -> renameSubjectAction.accept(subject,replacement)));
            remove.setOnAction(e -> {Alert confirmation=new Alert(Alert.AlertType.CONFIRMATION,"Delete subject “"+subject+"”?",ButtonType.OK,ButtonType.CANCEL);confirmation.initOwner(getScene().getWindow());styleDialog(confirmation);if(confirmation.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK) deleteSubjectAction.accept(subject);});
            MenuButton menu=new MenuButton("⋮",null,rename,remove);menu.getStyleClass().add("quiet");menu.setAccessibleText("Actions for "+subject);
            Button open=new Button("View tasks →");open.getStyleClass().add("link-button");open.setOnAction(e -> {status.setValue("All statuses");search.clear();course.setValue(subject);showPage("Tasks");});
            VBox content=new VBox(14,new HBox(symbol,spacer(),menu),label(subject,"section-title"),label(tasks.size()+" tasks  ·  "+done+" completed","small muted"),progressRow,open);content.setPrefWidth(260);content.getStyleClass().add("subject-card");tiles.getChildren().add(content);
        }
        if(courses.isEmpty()) return card("Your subjects will appear here",label("Add a subject to start organizing your coursework.","muted"));return scroll(tiles);
    }
    private Optional<String> subjectDialog(String original) {
        TextInputDialog dialog=new TextInputDialog(original==null?"":original);dialog.setTitle(original==null?"Add Subject":"Rename Subject");dialog.setHeaderText(original==null?"A place for your next course":"Rename “"+original+"” and its tasks");dialog.setGraphic(null);dialog.setContentText("Subject name");dialog.initOwner(getScene().getWindow());styleDialog(dialog);dialog.getDialogPane().setMinWidth(420);dialog.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().add("primary");
        return dialog.showAndWait();
    }
    private Node analytics() {
        long done=snapshot.stream().filter(t -> t.getStatus()==TaskStatus.COMPLETED).count();
        Label explanation=label("Live summary of saved tasks. Weekly charts use due dates, rather than historical completion times.","muted");explanation.setWrapText(true);
        CategoryAxis x=new CategoryAxis();NumberAxis y=new NumberAxis();y.setMinorTickVisible(false);y.setLabel("Tasks");
        BarChart<String,Number> workload=new BarChart<>(x,y);workload.setAnimated(false);workload.setLegendVisible(false);workload.setPrefHeight(260);
        XYChart.Series<String,Number> series=new XYChart.Series<>();LocalDate monday=LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for(int i=0;i<7;i++) {LocalDate date=monday.plusDays(i);series.getData().add(new XYChart.Data<>(date.format(DateTimeFormatter.ofPattern("EEE",Locale.ENGLISH)),snapshot.stream().filter(t -> t.getDeadline().equals(date)).count()));}workload.getData().add(series);
        double peak=series.getData().stream().mapToDouble(point -> point.getYValue().doubleValue()).max().orElse(0);
        y.setAutoRanging(false);y.setLowerBound(0);y.setUpperBound(Math.max(1,peak+1));y.setTickUnit(Math.max(1,Math.ceil((peak+1)/5)));
        PieChart distribution=new PieChart();distribution.setAnimated(false);distribution.setLabelsVisible(false);distribution.setPrefHeight(260);
        snapshot.stream().map(Task::getCourse).distinct().sorted().forEach(c -> distribution.getData().add(new PieChart.Data(c,snapshot.stream().filter(t -> t.getCourse().equals(c)).count())));
        VBox left=card("Due this week",workload),right=card("Subject distribution",distribution);growCards(left,right);
        VBox completion=card("Completion rate",progressRing(done,snapshot.size()));VBox breakdown=card("Task status",legend("Completed",done,"green"),legend("In progress",snapshot.stream().filter(t -> t.getStatus()==TaskStatus.IN_PROGRESS && !overdue(t)).count(),"amber"),legend("To do",snapshot.stream().filter(t -> t.getStatus()==TaskStatus.PENDING && !overdue(t)).count(),"blue"),legend("Overdue",snapshot.stream().filter(this::overdue).count(),"red"));growCards(completion,breakdown);
        VBox body=new VBox(18,explanation,new HBox(18,left,right),new HBox(18,completion,breakdown));if(snapshot.isEmpty()) body.getChildren().add(0,label("Add tasks to see your study patterns here.","empty-note"));return scroll(body);
    }
    private void growCards(VBox...cards) {for(VBox c:cards) {c.setPrefWidth(420);c.setMinWidth(200);HBox.setHgrow(c,javafx.scene.layout.Priority.ALWAYS);}}

    private Node settings() {
        VBox sections=new VBox(8);sections.setPrefWidth(165);sections.setMinWidth(150);
        for(String name:List.of("General","Data","About")) {Button b=new Button(name,icon(name.equals("General")?"settings":name.equals("Data")?"tasks":"home"));b.getStyleClass().addAll("settings-tab",settingsSection.equals(name)?"settings-selected":"quiet");b.setMaxWidth(Double.MAX_VALUE);b.setOnAction(e -> {settingsSection=name;showPage("Settings");});sections.getChildren().add(b);}
        VBox panel;
        if(settingsSection.equals("About")) {
            panel=new VBox(15,logo(130),label("StudyTask","about-title"),label("Version 1.0.0","task-title"),label("Organize  ·  Plan  ·  Achieve","muted"),label("A simple, focused study planner.","muted"));panel.setAlignment(Pos.CENTER);panel.setPadding(new Insets(60));panel.getStyleClass().add("card");
        } else if(settingsSection.equals("Data")) {
            Label path=label(TaskStorage.defaultPath().toString(),"data-path");path.setWrapText(true);Label detail=label("Tasks save automatically after changes. Keep a copy of tasks.json as a backup. Your tasks stay separate from the installation and remain available when the app is updated.","muted");detail.setWrapText(true);
            panel=card("Your local data",label("Storage location","task-title"),path,detail);
        } else {
            HBox themes=new HBox(12);ToggleGroup group=new ToggleGroup();
            for(String theme:List.of("Light","Dark","System")) {ToggleButton b=new ToggleButton(theme,icon(theme.equals("Dark")?"moon":theme.equals("Light")?"sun":"screen"));b.setContentDisplay(ContentDisplay.TOP);b.setToggleGroup(group);b.getStyleClass().add("theme-option");b.setSelected(preferences.get("theme","Light").equals(theme));b.setOnAction(e -> {preferences.put("theme",theme);applyAppearance();b.setSelected(true);});themes.getChildren().add(b);}
            HBox accents=new HBox(12);for(String color:COLORS) {Button b=new Button();b.getStyleClass().add("accent-swatch");b.setStyle("-fx-background-color: "+color+";");b.setAccessibleText("Use accent "+color);b.setOnAction(e -> {preferences.put("accent",color);applyAppearance();});accents.getChildren().add(b);}
            panel=card("Appearance",label("Theme","task-title"),themes,label("Accent color","task-title"),accents,label("Appearance preferences are saved for your next visit.","small muted"));
        }
        panel.setMaxWidth(Double.MAX_VALUE);HBox.setHgrow(panel,javafx.scene.layout.Priority.ALWAYS);HBox row=new HBox(24,sections,panel);row.setAlignment(Pos.TOP_LEFT);return scroll(row);
    }
    private void applyAppearance() {
        String theme=preferences.get("theme","Light");boolean dark=theme.equals("Dark") || theme.equals("System") && systemDarkTheme();
        getStyleClass().remove("dark");if(dark) getStyleClass().add("dark");setStyle("-app-accent: "+preferences.get("accent",COLORS[0])+";");
    }
    private boolean systemDarkTheme() {
        if(!System.getProperty("os.name","").startsWith("Windows")) return false;
        try {
            Process query=new ProcessBuilder("reg.exe","query","HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize","/v","AppsUseLightTheme").redirectErrorStream(true).start();
            if(!query.waitFor(2,java.util.concurrent.TimeUnit.SECONDS)) {query.destroyForcibly();return false;}
            return new String(query.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).matches("(?s).*AppsUseLightTheme\\s+REG_DWORD\\s+0x0\\s*.*");
        } catch(java.io.IOException ex) {return false;} catch(InterruptedException ex) {Thread.currentThread().interrupt();return false;}
    }

    public Optional<Task> taskDialog(Task original) {
        Dialog<Task> d=new Dialog<>();d.setTitle(original==null?"Add Task":"Edit Task");d.initOwner(getScene().getWindow());
        ButtonType save=new ButtonType(original==null?"Add Task":"Save Changes",ButtonBar.ButtonData.OK_DONE);d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,save);
        TextField title=new TextField(original==null?"":original.getTitle());title.setPromptText("e.g. Read Chapter 4");title.setAccessibleText("Title");
        TextField subject=new TextField(original==null?(course.getValue().equals("All courses")?"":course.getValue()):original.getCourse());subject.setPromptText("e.g. Mathematics");subject.setAccessibleText("Subject");
        DatePicker date=new DatePicker(original==null?LocalDate.now():original.getDeadline());date.setAccessibleText("Due date");date.setMaxWidth(Double.MAX_VALUE);
        ComboBox<Priority> importance=new ComboBox<>(FXCollections.observableArrayList(Priority.values()));importance.setValue(original==null?Priority.MEDIUM:original.getPriority());importance.setMaxWidth(Double.MAX_VALUE);importance.setAccessibleText("Priority");
        ComboBox<TaskStatus> taskStatus=new ComboBox<>(FXCollections.observableArrayList(TaskStatus.values()));taskStatus.setValue(original==null?TaskStatus.PENDING:original.getStatus());taskStatus.setAccessibleText("Completion status");taskStatus.setMaxWidth(Double.MAX_VALUE);
        friendlyCombo(importance);friendlyCombo(taskStatus);
        TextArea description=new TextArea(original==null?"":original.getDescription());description.setPromptText("Add details…");description.setPrefRowCount(4);description.setWrapText(true);description.setAccessibleText("Description");
        Label error=label("","validation");error.setWrapText(true);HBox details=new HBox(14,field("Subject",subject),field("Due date",date),field("Priority",importance));
        for(Node child:details.getChildren()) {HBox.setHgrow(child,javafx.scene.layout.Priority.ALWAYS);((VBox)child).setPrefWidth(160);}
        VBox fields=new VBox(18,label(original==null?"Add Task":"Edit Task","section-title"),field("Title",title),field("Description (optional)",description),details,field("Status",taskStatus),error);fields.setPrefWidth(560);d.getDialogPane().setContent(fields);styleDialog(d);
        d.getDialogPane().lookupButton(save).getStyleClass().add("primary");final Task[] result=new Task[1];
        d.getDialogPane().lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION,e -> {
            try {
                if(title.getText().isBlank()) throw new IllegalArgumentException("Enter a title for your task.");if(subject.getText().isBlank()) throw new IllegalArgumentException("Enter a subject or course name.");
                if(date.getEditor().getText().isBlank()) throw new IllegalArgumentException("Choose a due date.");date.commitValue();if(date.getValue()==null) throw new IllegalArgumentException("Enter a valid due date.");
                result[0]=new Task(original==null?UUID.randomUUID().toString():original.getId(),title.getText(),subject.getText(),date.getValue(),importance.getValue(),description.getText(),taskStatus.getValue());
            } catch(RuntimeException ex) {error.setText(ex.getMessage()==null?"Check the task details.":ex.getMessage());e.consume();}
        });d.setResultConverter(button -> button==save?result[0]:null);return d.showAndWait();
    }
    private void styleDialog(Dialog<?> d) {
        d.getDialogPane().getStylesheets().add(TaskView.class.getResource("/styles.css").toExternalForm());d.getDialogPane().getStyleClass().add("task-dialog");if(getStyleClass().contains("dark")) d.getDialogPane().getStyleClass().add("dark");d.getDialogPane().setStyle(getStyle());
    }
    public boolean confirmDelete(Task t) {Alert a=new Alert(Alert.AlertType.CONFIRMATION,"Delete “"+t.getTitle()+"”?",ButtonType.OK,ButtonType.CANCEL);a.setHeaderText("Delete task?");a.initOwner(getScene().getWindow());styleDialog(a);return a.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;}
    public void error(String message) {Alert a=new Alert(Alert.AlertType.ERROR);a.setHeaderText("StudyTask needs your attention");a.setContentText(message);a.initOwner(getScene().getWindow());styleDialog(a);a.showAndWait();}
    private static Label label(String text,String styles) {Label l=new Label(text);l.getStyleClass().addAll(styles.split(" "));return l;}
    private static VBox field(String title,Node control) {return new VBox(8,label(title,"field-label"),control);}
    private static VBox card(String title,Node...children) {VBox c=new VBox(14,label(title,"section-title"));c.getChildren().addAll(children);c.getStyleClass().add("card");return c;}
    private static Region spacer() {Region r=new Region();HBox.setHgrow(r,javafx.scene.layout.Priority.ALWAYS);return r;}
    private static Node scroll(Node content) {ScrollPane s=new ScrollPane(content);s.setFitToWidth(true);s.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);s.getStyleClass().add("page-scroll");return s;}
    private static ImageView logo(int size) {ImageView v=new ImageView(new Image(TaskView.class.getResource("/icons/studytask.png").toExternalForm()));v.setFitWidth(size);v.setFitHeight(size);v.setPreserveRatio(true);return v;}
    private static String pretty(String value) {return value.charAt(0)+value.substring(1).toLowerCase(Locale.ROOT);}
    private static <E extends Enum<E>> void friendlyCombo(ComboBox<E> combo) {
        combo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(E value) {
                if(value==null) return "";
                if(value.name().equals("PENDING")) return "To Do";
                return Arrays.stream(value.name().split("_")).map(TaskView::pretty).collect(java.util.stream.Collectors.joining(" "));
            }
            @Override public E fromString(String text) {return combo.getItems().stream().filter(value -> toString(value).equals(text)).findFirst().orElse(null);}
        });
    }
    private static Node icon(String name) {
        String path=switch(name) {
            case "home" -> "M2 10 L10 3 L18 10 M4 9 V18 H8 V13 H12 V18 H16 V9";
            case "tasks" -> "M6 4 H4 V18 H17 V4 H14 M8 2 H13 V6 H8 Z M7 10 L8 11 L10 9 M12 10 H14 M7 14 L8 15 L10 13 M12 14 H14";
            case "calendar" -> "M3 5 H17 V18 H3 Z M3 9 H17 M7 2 V7 M13 2 V7 M7 12 H9 M12 12 H14 M7 15 H9";
            case "subjects" -> "M3 3 H9 Q11 3 11 5 V18 Q9 16 3 17 Z M11 5 Q12 3 18 3 V17 Q13 16 11 18 M6 7 H8 M6 10 H8";
            case "analytics" -> "M3 3 V18 H18 M6 15 V11 M10 15 V7 M14 15 V4";
            case "settings" -> "M8 2 H12 L13 5 L16 5 L18 8 L16 11 L17 14 L14 17 L11 16 L8 18 L5 16 L5 13 L2 11 L3 7 L6 6 Z M13 10 A3 3 0 1 0 7 10 A3 3 0 1 0 13 10";
            case "plus" -> "M10 3 V17 M3 10 H17";
            case "check" -> "M4 10 L8 14 L17 5";
            case "clock" -> "M18 10 A8 8 0 1 0 2 10 A8 8 0 1 0 18 10 M10 5 V10 L14 12";
            case "moon" -> "M15 3 A8 8 0 1 0 18 14 A8 8 0 0 1 15 3";
            case "sun" -> "M13 10 A3 3 0 1 0 7 10 A3 3 0 1 0 13 10 M10 0 V3 M10 17 V20 M0 10 H3 M17 10 H20 M3 3 L5 5 M15 15 L17 17 M3 17 L5 15 M15 5 L17 3";
            default -> "M2 3 H18 V15 H2 Z M10 15 V19 M6 19 H14";
        };
        SVGPath svg=new SVGPath();svg.setContent(path);svg.getStyleClass().add("line-icon");StackPane box=new StackPane(svg);box.setMinSize(20,20);box.setPrefSize(20,20);box.setMaxSize(20,20);box.setMouseTransparent(true);return box;
    }
}
