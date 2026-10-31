# Windows packaging

Use a Java **21 JDK** containing javac, jlink, jmods and jpackage. Java 21's Windows EXE packaging requires **WiX 3.x**, with `candle.exe` and `light.exe` on PATH. WiX 4/5 support was introduced in later JDKs; do not substitute it for Java 21's requirement. Build on Windows, using a JDK whose native architecture matches the target. This is not cross-compilation.

Sources: [Oracle Java 21 packaging prerequisites](https://docs.oracle.com/en/java/javase/21/jpackage/packaging-overview.html), [jpackage options](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html), [JavaFX 21 platforms](https://docs.oracle.com/en/java/java-components/javafx/21/release-notes/), [Liberica supported configurations](https://bell-sw.com/pages/supported-configurations/).

## Architecture commands

Run each in a separate checkout/output folder, or archive `dist` between builds. Output names stay as requested; build-info.txt records the selected architecture.

```powershell
# x86-64 / AMD64 (also usable under Windows ARM emulation, but still an x64 build)
.\build-exe.bat -Architecture x64 -JdkHome "C:\JDKs\jdk-21-x64"

# Native ARM64: full vendor JDK with JavaFX 21, native jpackage and jlink
.\build-exe.bat -Architecture arm64 -JdkHome "C:\JDKs\jdk-21-arm64-full" -BundledJavaFx

# Legacy 32-bit Intel x86: full 32-bit Java 21 + JavaFX distribution
.\build-exe.bat -Architecture x86 -JdkHome "C:\JDKs\jdk-21-x86-full" -BundledJavaFx

# Portable image only; does not require WiX
.\build-exe.bat -Architecture x64 -ImageOnly
```

The default Maven dependencies are the public OpenJFX Windows x64 build. ARM64/x86 require the vendor-bundled profile and architecture-appropriate full JDK, such as a suitable Liberica Full release where available. Confirm the exact bundle includes JavaFX 21, `jmods`, `jlink.exe`, `jpackage.exe` and native launcher support. If a vendor omits a tool or jpackage cannot produce that architecture, that build is unsupported until a suitable distribution is provided. No x64 binary is relabeled as ARM64 or x86. Windows 11 has no 32-bit OS edition; a 32-bit release targets a compatible Windows version supported by its JDK vendor.

The script validates Java version and PE machine types for Java and JavaFX. It runs `mvnw.cmd -B clean verify` (plus `-Pbundled-javafx` where selected), staging the application JAR and runtime dependencies.

Every invocation also writes a transcript to `logs/build-exe-<architecture>-<timestamp>.log`, while keeping output visible in the terminal. Previous verification logs are stored in the same `logs/` folder. For a separate test log, use `.\mvnw.cmd -B test *> logs\tests.log` in PowerShell.

The packaging commands are:

```text
jlink --module-path <JDK>/jmods --add-modules java.base,java.desktop,java.logging,java.xml,java.prefs,jdk.unsupported,jdk.crypto.ec --output target/runtime --strip-debug --no-header-files --no-man-pages --compress=2
jpackage --type app-image --name StudyTask --app-version 1.0.0 --vendor StudyTask --input target/package-input --main-jar studytask-1.0.0.jar --main-class com.studytask.Main --runtime-image target/runtime --dest dist
jpackage --type exe --name StudyTask --app-version 1.0.0 --vendor StudyTask --app-image dist/StudyTask --dest target/installer --win-per-user-install --win-dir-chooser --win-menu --win-shortcut --win-upgrade-uuid 9eae3ba1-d8d2-4f9d-a93e-6260cc07efea
```

Bundled-JavaFX builds add `javafx.controls` to jlink roots and `--java-options --add-modules=javafx.controls` to the launcher. Central-profile builds distribute JavaFX JARs and their embedded natives on the application's classpath. Jackson reflection needs no module opens because application libraries are in the unnamed module. The runtime includes desktop/XML/logging/unsupported modules used by JavaFX/Jackson and EC cryptography. The installer generated as `StudyTask-1.0.0.exe` is renamed to `StudyTask-Setup.exe`.

## Release verification

1. Check `dist/StudyTask/StudyTask.exe`, `app/`, `runtime/` and the installer exist. Check build-info.txt and launcher/runtime machine types.
2. Double-click the portable EXE on a clean machine without Java/Maven, using the target architecture. Verify all seven task operations and persistence after restart.
3. Run the installer, verify shortcut launch, directory choice and uninstall.
4. Install a newer version using the same upgrade UUID; verify saved tasks remain. Increase `--app-version` for a release upgrade. Preserve the JSON schema or supply a migration before incompatible changes.
5. Test ARM64, x64 and x86 releases separately. Cross-architecture installer upgrade behavior is not guaranteed; back up data and uninstall the old architecture before switching.

Releases are unsigned. Code signing is a separate distribution step requiring the publisher's certificate. Do not infer launch/installation success merely from the presence of an EXE.
