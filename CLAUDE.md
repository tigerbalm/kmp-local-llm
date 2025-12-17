# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Multiplatform (KMP) project that provides a unified API for on-device LLM operations across Android and iOS. The library abstracts platform-specific LLM implementations into a common interface:
- **Android**: Gemini Nano via Google AI Client SDK (formerly ML Kit GenAI)
- **iOS**: Apple Foundation Models (iOS 18.1+)

## Build Commands

### Full Project Build
```bash
./gradlew build
```

### Module-Specific Builds
```bash
# Build core library
./gradlew :core_llm:build

# Build sample shared module
./gradlew :sampleShared:build

# Build Android app
./gradlew :androidApp:build
./gradlew :androidApp:installDebug

# Build iOS frameworks
./gradlew :core_llm:linkDebugFrameworkIosSimulatorArm64
./gradlew :sampleShared:linkDebugFrameworkIosSimulatorArm64
```

### Testing
```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :core_llm:test
```

### Clean
```bash
./gradlew clean
```

## Project Structure

The repository consists of three main modules:

### 1. core_llm (KMP Library)
The core abstraction library with platform-specific implementations. Key architecture:

- **commonMain**: Platform-agnostic API definitions
  - `LocalLlmClient` interface: Main entry point for all LLM operations
  - `LocalLlmClientFactory`: Factory using expect/actual pattern
  - `LlmRequest`/`LlmResponse`: Data models
  - `LlmCapability`: Feature detection enum
  - `LlmError`: Sealed class for error handling
  - `MetricsEnabledLlmClient`: Decorator pattern for optional observability

- **androidMain**: Gemini Nano implementation
  - `AndroidLocalLlmClient`: Wraps Google AI Client SDK's GenerativeModel
  - Uses on-device "gemini-nano" model (no API key required)
  - Implements streaming via Flow

- **iosMain**: Apple Foundation Models implementation
  - `IosLocalLlmClient`: Bridges to Swift via AppleLocalLlmBridge
  - Located in `src/appleBridge/AppleLocalLlmBridge.swift`
  - Uses Swift's async/await and AsyncSequence for streaming

### 2. sampleShared (KMP Sample Business Logic)
Shared ViewModel and repository layer for sample applications:

- `ChatViewModel`: Common state management using StateFlow
- `LlmRepository`: Wraps core_llm client with metrics
- `Message`/`ChatUiState`: UI state models
- `PresetPrompt`: Enum for quick actions (Summarize, Rewrite, Proofread, Explain)

### 3. androidApp & iosApp
Platform-specific UI implementations consuming sampleShared:
- **androidApp**: Jetpack Compose with Material 3
- **iosApp**: SwiftUI (separate Xcode project)

## Key Architecture Patterns

### expect/actual Pattern
The library uses Kotlin's expect/actual mechanism for platform abstraction:
```kotlin
// commonMain: Declaration
internal expect fun createPlatformClient(): LocalLlmClient

// androidMain/iosMain: Platform-specific implementations
internal actual fun createPlatformClient(): LocalLlmClient = AndroidLocalLlmClient(...)
```

### Factory Pattern
`LocalLlmClientFactory.create()` is the single entry point that returns the appropriate platform implementation.

### Decorator Pattern
`MetricsEnabledLlmClient` wraps any `LocalLlmClient` to add timing and token metrics without modifying the core implementation.

### Repository Pattern
`LlmRepository` (in sampleShared) provides a clean separation between the UI layer and the core_llm library.

## Platform-Specific Considerations

### Android
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: 34
- **JVM Target**: 17
- **Model**: Gemini Nano must be downloaded via AICore (not bundled)
- **Context Management**: Uses singleton `AndroidContextHolder` for context access
- **Dependency**: `com.google.ai.client.generativeai:generativeai:0.1.2`

### iOS
- **Minimum Version**: iOS 18.1+ (required for Apple Intelligence/LanguageModels)
- **Framework**: LanguageModels (native, not included as dependency)
- **Bridge**: Swift-Kotlin interop via `AppleLocalLlmBridge.swift`
- **Framework Output**: Static frameworks in `build/bin/ios{Platform}/debugFramework/`
- **Note**: Real device recommended; simulator support may be limited

## Streaming Architecture

The library provides streaming support through Kotlin Flow:

**Android**: Google AI SDK returns Flow<GenerateContentResponse>, mapped to Flow<String>

**iOS**: Swift's AsyncSequence is bridged to Kotlin Flow via callbackFlow

**Common**: `LocalLlmClient.streamText()` returns `Flow<String>` consistently across platforms

## Testing Approach

- **commonTest**: Data model tests and interface contracts
- Platform implementations tested with mock responses during development
- Real device/model testing required for integration verification
- No instrumented tests currently in place

## Error Handling Strategy

All platform-specific errors are mapped to a common sealed class hierarchy:
```kotlin
sealed class LlmError : Exception() {
    class ModelNotAvailable
    class SafetyBlocked
    class PermissionDenied
    class InternalError
}
```

Platform implementations catch native exceptions and map them to these common types.

## Development Workflow

1. **Common API changes**: Modify interfaces in `core_llm/src/commonMain`
2. **Platform updates**: Implement changes in both androidMain and iosMain
3. **Testing**: Run unit tests with `./gradlew test`
4. **Android testing**: Build and install with `./gradlew :androidApp:installDebug`
5. **iOS testing**: Build framework, then build/run Xcode project

## Important Notes

- The core library maintains 80%+ common code ratio
- Platform-specific code is minimal and focused on SDK bridging
- All async operations use suspend functions (single response) or Flow (streaming)
- No callbacks in the common API surface
- Metrics are optional via decorator pattern
- API is designed for local-first with future cloud fallback in mind