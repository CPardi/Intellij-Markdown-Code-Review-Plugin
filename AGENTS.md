# IntelliJ Platform Plugin Template

- Do not read the entire project, only files related to the task
- Do not read other plans or reviews only the one under consideration

## Tech Layers

- **Continuous Integration** GitHub Actions
  - Documentation: https://docs.github.com/en/actions
- **Build System**: Gradle
  - User Guide: https://docs.gradle.org/current/userguide/userguide.html
- **Framework**: IntelliJ Platform SDK
  - Documentation: https://plugins.jetbrains.com/docs/intellij/developing-plugins.html
- **Libraries**:
  - `com.intellij.openapi` source code: https://github.com/JetBrains/intellij-community/tree/master/platform/core-api/src/com/intellij/openapi
- **UI Framework**: Kotlin UI DSL
  - Guidelines: https://plugins.jetbrains.com/docs/intellij/ui-guidelines-welcome.html
  - Documentation: https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html
   - Tool Window Documentation: https://plugins.jetbrains.com/docs/intellij/tool-windows.html
- **Testing**:
  - Documentation: https://plugins.jetbrains.com/docs/intellij/testing-plugins.html

## Code Standards

### General Rules

- Respect the .editorconfig
- Never edit or delete this file (@[AGENTS.md])
- Never delete files in the @[plans/] directory
- Additional files are welcome if it aids clean separation
- Avoid nulls where possible
- Keep fields minimal, do not add properties in anticipation of future features.
- API features should only be used if they are available to
  - Community and paid editions
  - All IDE flavours (Intellij, Rider, etc)
  - Any minimum IDE version — use the latest IntelliJ Plugin APIs

### Early Development Stage

- Do not consider breaking changes
- Do not attempt to migrate data

### Fields

- Prefer immutable fields

### Strings

- User facing strings should be added to @[src/main/resources/messages/ReviewBundle.properties]

### Newlines and spacing

- Include a newline at the end of files

### Comments

- Write comments using British English
- Use inline comments sparingly and only to explain unobvious behaviour
- Don't include comments describing the specifics of the current change.
- Do not add comments that repeat a definition's name and type.
- Do not add banner or suite comments above classes

### Naming

- Classes containing unit tests should have `Tests` postfix
- Classes containing Light Platform tests should have `TestSuite` postfix

### Testing

- Do not test private definitions, only the public API should be tested
- If unit tests are becoming cumbersome, consider splitting definitions into smaller "units"
- If code covered by unit tests, then do not write smoke tests
- If private fields need to be queried or manipulated, then make them public. Do not use reflection.
- When writing Light Platform tests follow the pattern used in src/test/kotlin/com/github/cpardi/markdowncodereview/ui/ReviewToolWindowPanelTestSuite.kt
- Use XML files as virtual files for testing with inline XML snippets in test code
- Only check logic in UI tests, not static UI construction
- Use parameterized tests when multiple test methods share identical setup, execution, and assertions with only input values differing.

## GitHub Actions

GitHub actions are @[.github/workflows/] directory
Validate GitHub actions using running `actionlint`. Ensure any validation errors or warnings are fixed.
