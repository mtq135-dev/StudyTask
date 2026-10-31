# Task ADT

Task represents one identified academic activity. Its RI is nonblank id/title/course and nonnull deadline/priority/description/status. The constructor enforces the invariant; text identity fields are stripped. Empty description and past deadlines are valid. Its AF maps these fields to the corresponding task in the student's planner.

The final class stores private final String, LocalDate and enum values. These types are immutable; getters cannot expose mutable state. Each with-method constructs a validated replacement preserving id, leaving the original untouched. Editing similarly constructs a new value with the existing identifier.

Equality and hashCode use id alone. Changing a title or status therefore does not change logical identity or HashSet membership. Different versions of the same task may compare equal even when other fields differ. Tests cover the equality contract and immutable updates. The model keeps mutable organization private and returns defensive snapshots.
