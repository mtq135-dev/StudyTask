# Verification record

Verified through 2026-10-31, Windows ARM64 host running Temurin Java 21.0.12.1 x64 under emulation:

- Official Maven Wrapper generated, pinned to Maven 3.9.9; wrapper `verify` passed.
- The required `mvnw.cmd javafx:run` command opened a separate StudyTask window using the selected JDK; main controls were confirmed through Windows accessibility.
- Final redesign build: all 12 JUnit tests passed, with 0 failures/errors/skips. Tests cover ADT validation/equality/immutability, model operations/defensive snapshots, Observer behavior, subject management, subject-file corruption protection and In Progress persistence after restart.
- `build-exe.bat` completed with WiX 3.11.2 portable tools on PATH, including another successful test run, jlink and both jpackage stages.
- Final x64 portable launcher generated at `dist/StudyTask/StudyTask.exe` (498,176 bytes), with bundled runtime and dependencies present. The runtime's module list includes java.prefs for saved appearance preferences. Launched the packaged EXE and inspected the reference-style Home, Tasks, Calendar, Subjects, Analytics and Settings screens.
- ARM64 and x86 preflight rejection tested with this x64 JDK: both correctly returned exit code 1 instead of producing mislabeled binaries.
- The PNG has an RGBA alpha range of 0–255. Seven ICO image frames are embedded in the launcher and installer; icon resources are present in the application JAR.
- Current x64 installer generated at `dist/StudyTask-Setup.exe` (46,384,128 bytes). SHA256: `E830E59D74062CB6D1D5C126FCC1783EE485BD648A9EC329B2421971805F7A1B`.
- GUI checks used isolated data under `.tools/ui-qa`, rather than the user's application-data folder. Blank task submission displayed a validation message. Adding a task saved it to JSON and it reappeared after closing/reopening the EXE. Adding an empty subject saved it to subjects.json. Dark theme applied correctly, then Light was restored.
- Final EXE: In Progress showed 1 of 9 sample tasks; Reset restored 9 of 9; searching “Lab” showed only Lab Report with its title and course intact. Completing that filtered task changed its visible badge and persisted COMPLETED to JSON. Calendar task chips and analytics charts displayed the sample deadlines/counts.

Not verified: installer execution/install/uninstall/upgrades; clean-machine launch without a host JDK; every combination of UI actions, confirmation dialogs and window sizes; native ARM64 or 32-bit x86 build/launch. Matching full Java 21 toolchains for those architectures are not installed here. Script paths are provided with checks but are conditional on vendor support, not certified releases. An x64 application running on ARM under emulation is not native ARM64 support.
