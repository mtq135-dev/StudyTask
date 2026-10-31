# StudyTask icon

Source artwork supplied by the user. Background extraction used the built-in image generation tool, followed by PNG-to-ICO format conversion with Pillow. The transparent PNG is `src/main/resources/icons/studytask.png`; the multi-resolution Windows icon is `src/main/resources/icons/studytask.ico` (16, 24, 32, 48, 64, 128 and 256 pixels).

Final image prompt: Reproduce the supplied blue graduation-cap/task checklist/checkmark emblem as a clean vector-style app icon with actual transparency; remove the white rounded-square tile and shadow, preserve the blue shapes and white checkmark, and keep negative space transparent.

Main loads the PNG as the JavaFX Stage icon. Both jpackage commands receive the ICO through `--icon`, so future architecture-appropriate builds use the same artwork.
