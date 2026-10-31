# MVC architecture

```text
User -> TaskView controls -> TaskController -> TaskModel
                              ^                  |
                              +---- Observer ----+
                              |
                              +-> TaskView.render
                              +-> TaskStorage.save
```

TaskModel owns a LinkedHashMap, rejects duplicate/missing identifiers and returns immutable snapshots. It has no JavaFX imports. TaskView owns controls, validation messages and confirmation dialogs; it has no reference to the model. TaskController wires actions, applies combined course/status filters and coordinates storage. Main creates these objects and loads storage before showing the window. Controller observer callbacks marshal UI work to the JavaFX application thread. Normal user mutations occur on that thread; the model is not intended for concurrent access.

TaskStorage isolates Jackson and file operations for tasks.json and subjects.json. Safe replacement and failed-load write protection prevent accidental corruption loss. The two files are individually atomic; a save failure between writes is reported, rather than claiming a transactional two-file commit. Synchronous local I/O keeps this course implementation simple; very large files could briefly block the UI.

TaskModel also owns independently created subject names; its course list combines those names with courses used by tasks. Rename changes all affected immutable Task values in one mutation and sends one notification. The View holds presentation snapshots for dashboard/calendar/charts, never the authoritative collection. TaskController wires subject actions and search as well as the original task operations. Light/dark/accent preferences are presentation state saved with Java Preferences; the custom runtime includes java.prefs.
