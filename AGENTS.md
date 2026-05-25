# IntelliJ Platform Plugin Template

## Tech Layers

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

- Never edit or delete this file (AGENTS.md)
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
- Do not add tests

### Fields

- Prefer immutable fields

### Strings

- User facing strings should be added to src/main/resources/messages/ReviewBundle.properties

### Newlines and spacing

- Include a newline at the end of files

### Comments

- Use inline comments sparingly and only to explain unobvious behaviour
- Don't include comments describing specific to the current change. 