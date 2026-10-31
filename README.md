# StudyTask

A Java 21 / JavaFX desktop planner for university coursework. Add, edit, delete (with confirmation), complete, reprioritize and filter tasks by course and status. Descriptions are available in the edit dialog. There are no servers or databases.

## Desktop workspace

The interface follows the supplied reference: a dark navigation sidebar, light workspace, blue accents, status badges and compact cards. All six navigation destinations use saved local data:

- **Home:** tasks due today, in-progress/completed/overdue counts, current completion ring, focus and upcoming task lists. Open a task from either list.
- **Tasks:** search title, subject and description; combine subject filters with All, To Do, In Progress, Completed or Overdue tabs. Select a task for editing, status/priority updates or confirmed deletion; double-click to edit.
- **Calendar:** browse months, return to today and open tasks from due-date chips. A “more” button exposes additional tasks on busy dates.
- **Subjects:** create empty subjects, rename subjects together with their tasks, view course-filtered tasks and delete empty subjects with confirmation. Progress is derived from task completion.
- **Analytics:** actual due-date workload for the current week, subject distribution, completion rate and status counts. These are current snapshots, not invented completion-history statistics.
- **Settings:** Light, Dark and System appearance, six accent colors, storage information and About. Appearance preferences persist through Java Preferences. System theme is read from Windows at startup or when System is selected.

To Do maps to `PENDING`; `IN_PROGRESS` and `COMPLETED` are persisted statuses. Overdue is calculated for unfinished tasks with deadlines before today. Existing `PENDING`/`COMPLETED` JSON files remain compatible. Independently created subject names are stored in `subjects.json` next to `tasks.json`, with the same failed-load protection and atomic replacement behavior. Back up both files. Subject renames update task course values without changing task identifiers.

## Run the app

Double-click `StudyTask.exe` in the project root. Keep the `app/` and `runtime/` folders beside it: they contain the application files and bundled Java runtime. No separate Java or Maven installation is required to run this packaged app.

The setup installer has been removed. Launch the app directly from this folder.

## Development

Install a Windows x64 Java 21 JDK and set JAVA_HOME. Maven is bootstrapped by the included Apache Maven Wrapper; first use requires internet access. No global Maven installation is needed.

```powershell
.\mvnw.cmd javafx:run
.\mvnw.cmd test
.\mvnw.cmd verify
```

Use the Maven command above to run from source; `start.bat` has been removed. Both the packaged launcher and Maven use `com.studytask.Main`.

For native ARM64 or 32-bit x86 development, select an architecture-matching full Java 21 JDK containing JavaFX 21. The bundled profile activates automatically when its JavaFX controls JMOD exists, so the standard `javafx:run` command also works with that full JDK. Explicit selection is available:

```powershell
$env:JAVA_HOME = 'C:\path\to\matching-full-jdk-21'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd -Pbundled-javafx javafx:run
.\mvnw.cmd -Pbundled-javafx test
```

Do not use the default Central JavaFX profile on ARM64/x86 JDKs: its Windows natives are x64. A full JDK profile uses JavaFX modules supplied by the selected JDK. Availability of JavaFX, jlink and jpackage in that exact vendor/architecture/version must be checked. A profile or script option alone is not evidence of a working native release.

## Windows executable builds

See [packaging commands and platform requirements](docs/packaging.md). Standard x64 build:

```powershell
.\build-exe.bat
```

`build-exe.bat` and `scripts/build-exe.ps1` are retained to rebuild the executable after source changes. A build produces a portable `dist/StudyTask/StudyTask.exe` with `app/` and `runtime/`. Installer creation also requires WiX 3.x (`candle.exe` and `light.exe`) on PATH and produces `dist/StudyTask-Setup.exe`. The local WiX tools were removed during cleanup; install them separately if you need an installer. To build only the portable app:

```powershell
.\build-exe.bat -ImageOnly
```

End users need neither Java nor Maven. Builds include JUnit tests. Failures return a nonzero exit code. The installer is per-user, with shortcuts and a directory chooser.

## Design and data

`TaskModel` owns the task collection and business rules without JavaFX dependencies. `TaskView` constructs controls and dialogs without modifying the model. `TaskController` translates user actions and subscribes to the model's Observer notifications. On changes it updates the filtered view on the JavaFX thread and saves JSON. Failed saves are reported; the in-memory edit remains available until exit.

`Task` is a final immutable ADT with private final fields and validated construction. The representation invariant requires nonblank identity/title/course and nonnull remaining values. The abstraction function maps those fields to one university task. Updates create new values preserving identity. Equality and hashing use only the identifier, so edited versions represent the same logical task.

Windows task data is `%LOCALAPPDATA%\StudyTask\tasks.json`; fallback is `~/.local/share/StudyTask/tasks.json`. This path is independent of installation folder, executable architecture and version, preserving data through updates. Save uses a temporary file and atomic replacement where supported. Invalid JSON or duplicate identifiers disable saving for that session, preserving the original file. Back up and repair/rename the damaged file, then restart. Uninstall does not remove this data. Avoid opening several instances simultaneously: there is no interprocess merge or lock.

## Structure and course documentation

`src/main/java/com/studytask/` contains Main, model, view, controller, observer and persistence packages. `src/main/resources/styles.css` defines the light theme. `src/test/java/com/studytask/` contains JUnit 5 tests that do not start JavaFX. `scripts/build-exe.ps1` implements the batch packaging entry point.

The cleaned workspace keeps:

- `StudyTask.exe`, `app/`, and `runtime/` to run the packaged app.
- `src/`, `pom.xml`, `.mvn/`, `mvnw`, and `mvnw.cmd` for source development and builds.
- `build-exe.bat` and `scripts/` for executable packaging.
- `README.md`, `docs/`, `.gitignore`, and `.git/` for documentation and version control.

Generated builds (`target/`, `dist/`), local development tools (`.tools/`), and old logs (`logs/`) were removed. Maven recreates `target/` when building; packaging recreates `dist/` and `logs/`. These generated folders are not needed to run the root-level executable. Packaging also generates `dist/build-info.txt`, which records build details and is not needed at runtime.

- [MVC architecture](docs/architecture.md)
- [Observer pattern](docs/design-pattern.md)
- [Immutable ADT specification](docs/adt-specification.md)
- [Requirements traceability](docs/requirements-traceability.md)

See [verification record](docs/verification.md) for actual results and outstanding manual checks.

Each `build-exe.bat` run automatically creates `logs/` and writes a timestamped packaging log there, including failed preflight checks. Generated `.log` files are ignored by Git. The verification record describes earlier checks; their generated logs were removed during cleanup.
