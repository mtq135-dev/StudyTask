# Observer

TaskObserver defines onTasksChanged(). TaskModel supports registration/removal, deduplicates registrations and iterates a snapshot of observers after successful mutations. Failed validation or missing/duplicate identifiers do not notify. TaskController implements the observer and derives the visible task list from the model before rendering TaskView. No user action manually refreshes after a model mutation. Filter changes refresh the presentation without mutating tasks.

This pattern decouples task state from JavaFX and keeps counters and filters consistent after edits. Tests verify multiple observers, removal, duplicate registration and failed-operation behavior. Observers should handle their own failures; the production controller reports storage errors through the view.
