# Test Infrastructure

This directory contains the test suite for the IntelliJ Review Markdown Generator plugin.

## Directory Structure

- `kotlin/com/gitlab/chripari/intellijreviewmarkdowngenerator/` - Test source code mirroring main package structure
  - `BaseTestHelper.kt` - Common test utilities and factory methods
  - `BaseTestHelperTest.kt` - Tests for the helper utilities
  - `UnitTest.kt` - Base class for pure Kotlin unit tests
  - `LightPlatformTest.kt` - Base class for IntelliJ Platform integration tests
  - `parser/` - Parser and writer tests
  - `services/` - Service and model tests
  - `actions/` - Action tests
  - `markers/` - Line marker provider tests
  - `ui/` - Dialog and panel tests
  - `settings/` - Settings tests
  - `startup/` - Startup activity tests
  - `util/` - Utility class tests

- `testData/` - Test fixtures and data files
  - `parser/` - Parser test markdown files
  - `writer/` - Writer test markdown files
  - `services/` - Service test data
  - `integration/` - Integration test fixtures

## Running Tests

Run all tests:
```bash
./gradlew test
```

Run specific test class:
```bash
./gradlew test --tests "ReviewFileParserSmokeTest"
```

View test reports:
```bash
open build/reports/tests/test/index.html
```

## Test Base Classes

### UnitTest
Use for tests that don't require IntelliJ Platform dependencies:
- Parser logic tests
- Writer logic tests
- Data model tests
- Utility class tests

### LightPlatformTest
Use for tests that require IntelliJ Platform integration:
- Service tests requiring Project or VirtualFile
- Action tests
- Line marker provider tests
- Document and PSI tests

## Test Utilities (BaseTestHelper)

### Factory Methods
- `createComment()` - Create line comments
- `createPageComment()` - Create page comments
- `createReviewFile()` - Create review files

### Assertion Helpers
- `assertCommentEquals()` - Compare comments with detailed failure messages
- `assertCommentListEquals()` - Compare comment lists
- `assertReviewFileEquals()` - Compare review files

### Content Builders
- `buildReviewMarkdown()` - Generate markdown from comments

### Resource Loading
- `loadTestResource()` - Load test fixtures from testData/
- `getTestResourcePath()` - Get path to test resources

## Phase 1 Implementation Status

✅ Gradle test configuration
✅ Test directory structure
✅ BaseTestHelper utility class
✅ LightPlatformTest base class
✅ UnitTest base class
✅ Smoke tests for parser, writer, and models
✅ Test data fixtures
✅ Documentation

See `plans/2026-05-22-phase1-test-infrastructure-v1.md` for detailed implementation plan.