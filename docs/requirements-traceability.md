# Requirements traceability

| Requirement | Implementation | Verification |
|---|---|---|
| FR-01 Add | TaskView.taskDialog, TaskController, TaskModel.addTask | TaskTest invalidValuesRejected; TaskModelTest; manually submit valid/blank fields |
| FR-02 View | TaskView.render, TaskModel.getAllTasks | TaskModelTest snapshots; manually inspect table and edit description |
| FR-03 Edit | taskDialog(existing), TaskModel.updateTask | TaskModelTest fullLifecycleAndFilters; manually edit under active filters |
| FR-04 Delete | confirmDelete, TaskModel.deleteTask | TaskModelTest; manually cancel and confirm deletion |
| FR-05 Status | TaskModel.updateTaskStatus | TaskModelTest; manually toggle twice |
| FR-06 Priority | TaskModel.updateTaskPriority | TaskModelTest; manually set all priorities |
| FR-07 Filter | TaskModel filter methods; TaskController.refresh combined filters | TaskModelTest; manually combine/reset filters after edits |

TaskObserverTest verifies mutation-driven updates without JavaFX. TaskStorageTest verifies missing files, round trips, duplicate identifiers and preservation of corrupted JSON. Packaging verification steps are in packaging.md. Automated model tests do not establish that the graphical dialogs or installer work.

The supplied desktop reference also maps to working Home, Calendar, Subjects, Analytics and Settings views in TaskView. Calendar and analytics derive values from model snapshots. Subject actions call TaskModel.addSubject/renameSubject/deleteSubject through TaskController; TaskModelTest covers subject identity preservation and safe removal. TaskStorageTest verifies independent subject persistence, corrupted subject-file protection and the new IN_PROGRESS status after restart. Appearance is presentation state saved through Java Preferences. Actual GUI checks and remaining release checks are recorded in verification.md.
