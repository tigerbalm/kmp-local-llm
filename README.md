# KMP Local LLM Abstraction Library

Kotlin Multiplatform library for abstracting Android and iOS on-device LLM capabilities into a unified API.

## Overview

This library provides a platform-agnostic interface for local LLM operations:
- **Android**: Gemini Nano (via ML Kit GenAI)
- **iOS**: Apple Foundation Models (iOS 18.1+)

### Key Features

- 🎯 **Platform Agnostic**: Single API for both Android and iOS
- 🔒 **Local-First**: All processing happens on-device
- 📊 **Built-in Metrics**: Optional observability layer
- 🌊 **Streaming Support**: Real-time text generation
- ⚡ **Coroutines & Flow**: Modern async API

## Installation

### Gradle (build.gradle.kts)

```kotlin
dependencies {
    implementation("com.thinq.kmp:core-llm:0.1.0")
}
```

### Android Setup

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Android context
        AndroidContextHolder.initialize(this)
    }
}
```

Add to `build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.google.mlkit:generative-ai:latest")
}
```

### iOS Setup

Requires iOS 18.1+ with Apple Intelligence enabled.

## Quick Start

### Basic Usage

```kotlin
import com.thinq.kmp.llm.factory.LocalLlmClientFactory
import com.thinq.kmp.llm.api.*

// Create client
val client = LocalLlmClientFactory.create()

// Check availability
if (client.isAvailable()) {
    // Generate text
    val response = client.generateText(
        LlmRequest(
            prompt = "Summarize: Kotlin is a modern programming language...",
            maxTokens = 512,
            temperature = 0.7
        )
    )
    println(response.text)
}
```

### Streaming

```kotlin
client.streamText(
    LlmRequest(prompt = "Write a short story about...")
).collect { chunk ->
    print(chunk) // Print each chunk as it arrives
}
```

### With Metrics

```kotlin
import com.thinq.kmp.llm.metrics.*

val baseClient = LocalLlmClientFactory.create()
val client = MetricsEnabledLlmClient(
    delegate = baseClient,
    reporter = ConsoleMetricsReporter()
)

// All requests are now tracked
val response = client.generateText(request)
```

## API Reference

### LocalLlmClient

Main interface for LLM operations.

#### Methods

- `suspend fun isAvailable(): Boolean` - Check if model is ready
- `suspend fun generateText(request: LlmRequest): LlmResponse` - Generate text
- `fun streamText(request: LlmRequest): Flow<String>` - Stream text generation

#### Properties

- `val capabilities: Set<LlmCapability>` - Supported features

### LlmRequest

```kotlin
data class LlmRequest(
    val prompt: String,
    val systemInstruction: String? = null,
    val maxTokens: Int = 512,
    val temperature: Double = 0.7,
    val metadata: Map<String, String> = emptyMap()
)
```

### LlmCapability

```kotlin
enum class LlmCapability {
    TEXT_GENERATION,
    SUMMARIZATION,
    REWRITING,
    PROOFREADING,
    STREAMING
}
```

### Error Handling

```kotlin
try {
    val response = client.generateText(request)
} catch (e: LlmError.ModelNotAvailable) {
    // Model not downloaded or unavailable
} catch (e: LlmError.SafetyBlocked) {
    // Content blocked by safety filters
} catch (e: LlmError.PermissionDenied) {
    // Permission issue
} catch (e: LlmError.InternalError) {
    // Other errors
}
```

## Platform-Specific Notes

### Android

- Requires Android API 24+
- Gemini Nano model must be downloaded via AICore
- Streaming support is limited
- Check device compatibility before deployment

### iOS

- Requires iOS 18.1+
- Apple Intelligence must be enabled in Settings
- Full streaming support via AsyncSequence
- LanguageModels framework required

## Architecture

```
┌─────────────────────────────────────┐
│    Feature Code (Common)            │
├─────────────────────────────────────┤
│    LocalLlmClient (Interface)       │
├──────────────┬──────────────────────┤
│   Android    │        iOS           │
│ ML Kit GenAI │  Foundation Models   │
└──────────────┴──────────────────────┘
```

## License

Copyright © 2024 ThinQ

## Roadmap

- [ ] Full ML Kit GenAI integration
- [ ] Cloud LLM fallback support
- [ ] Multimodal (image + text)
- [ ] Tool calling / Function calling
- [ ] Prompt template engine
