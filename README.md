# MongoDeclaration

[![JetBrains Marketplace](https://img.shields.io/badge/JetBrains%20Marketplace-MongoDeclaration-blue)][jb:marketplace]

MongoDeclaration is an IntelliJ Platform plugin for Groovy developers designed to replace dynamic `def` and `var` variable declarations with explicit, typesafe types whenever they can be statically inferred.

By transforming dynamic variable declarations into explicit static types, this plugin improves code readability, prevents type-related runtime defects, and optimizes IDE static analysis, autocompletion, and refactoring performance.

---

## Features

- **Automatic Type Inference:** Identifies Groovy `def` and `var` variable declarations where a concrete, uniform explicit type can be inferred from initialization expressions.
- **One-Click Quick-Fix:** Provides an intention/quick-fix (`Replace with '<Type>'`) to immediately swap `def` or `var` for the inferred type.
- **Modifier Preservation:** Seamlessly preserves existing modifiers such as `final` (e.g., `final def name = "MongoDB"` → `final String name = "MongoDB"`).
- **Multi-Variable Safety:** Handles multi-variable statements (e.g., `def a = "first", b = "second"` → `String a = "first", b = "second"`) while safely skipping declarations with mixed or unresolvable types.
- **False-Positive Prevention:** Ignores uninitialized variables, generic fallback `Object` types, and variables that are already explicitly typed.

---

## Example

### Before:
```groovy
def name = "MongoDB"
var port = 27017
final def hosts = ["localhost"]
```

### After Quick-Fix:
```groovy
String name = "MongoDB"
Integer port = 27017
final ArrayList hosts = ["localhost"]
```

---

## Installation & Requirements

- **Supported IDEs:** IntelliJ IDEA (Community & Ultimate) 2026.2+
- **Plugin Dependencies:** Requires bundled Groovy plugin (`org.intellij.groovy`).

### Building from Source

To build and test the plugin locally:

```bash
# Run test suite
./gradlew test

# Verify plugin compatibility and structural integrity
./gradlew check

# Run an isolated IDE sandbox with the plugin loaded
./gradlew runIde

# Build the distributable plugin archive (.zip)
./gradlew buildPlugin
```

---

## License

This project is licensed under the terms defined in the repository.

[jb:marketplace]: https://plugins.jetbrains.com
