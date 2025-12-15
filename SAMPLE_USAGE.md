# Sample Usage Examples

## Example 1: Simple Text Generation

```kotlin
import com.thinq.kmp.llm.factory.LocalLlmClientFactory
import com.thinq.kmp.llm.api.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = LocalLlmClientFactory.create()
    
    // Check if LLM is available
    if (!client.isAvailable()) {
        println("LLM is not available on this device")
        return@runBlocking
    }
    
    // Create request
    val request = LlmRequest(
        prompt = "Explain Kotlin Multiplatform in one sentence.",
        maxTokens = 100,
        temperature = 0.7
    )
    
    // Generate response
    try {
        val response = client.generateText(request)
        println("Response: ${response.text}")
        println("Tokens used: ${response.usage?.totalTokens}")
    } catch (e: LlmError) {
        println("Error: ${e.message}")
    }
}
```

## Example 2: Text Summarization

```kotlin
suspend fun summarizeText(text: String): String? {
    val client = LocalLlmClientFactory.create()
    
    if (!LlmCapability.SUMMARIZATION in client.capabilities) {
        println("Summarization not supported")
        return null
    }
    
    val request = LlmRequest(
        prompt = "Summarize the following text:\n\n$text",
        systemInstruction = "You are a helpful summarization assistant. Provide concise summaries.",
        maxTokens = 200
    )
    
    return try {
        val response = client.generateText(request)
        response.text
    } catch (e: LlmError.SafetyBlocked) {
        println("Content blocked by safety filters")
        null
    } catch (e: LlmError) {
        println("Error during summarization: ${e.message}")
        null
    }
}
```

## Example 3: Streaming Chat Interface

```kotlin
import kotlinx.coroutines.flow.collect

suspend fun streamingChat(userMessage: String) {
    val client = LocalLlmClientFactory.create()
    
    if (!LlmCapability.STREAMING in client.capabilities) {
        println("Streaming not supported, using regular generation")
        val response = client.generateText(LlmRequest(userMessage))
        println(response.text)
        return
    }
    
    val request = LlmRequest(
        prompt = userMessage,
        temperature = 0.8
    )
    
    print("AI: ")
    client.streamText(request).collect { chunk ->
        print(chunk)
    }
    println()
}

// Usage
fun main() = runBlocking {
    streamingChat("Tell me a short joke about programmers")
}
```

## Example 4: With Metrics and Error Handling

```kotlin
import com.thinq.kmp.llm.metrics.*

class MyMetricsReporter : LlmMetricsReporter {
    override fun onRequestStart(capability: LlmCapability, metadata: Map<String, String>) {
        // Send to analytics
        analytics.track("llm_request_started", mapOf("capability" to capability.name))
    }
    
    override fun onRequestSuccess(capability: LlmCapability, durationMs: Long, tokenCount: Int?) {
        analytics.track("llm_request_success", mapOf(
            "capability" to capability.name,
            "duration_ms" to durationMs,
            "tokens" to tokenCount
        ))
    }
    
    override fun onRequestFailure(capability: LlmCapability, error: LlmError, durationMs: Long) {
        analytics.track("llm_request_failure", mapOf(
            "capability" to capability.name,
            "error" to error.javaClass.simpleName,
            "duration_ms" to durationMs
        ))
    }
    
    override fun onAvailabilityCheck(isAvailable: Boolean) {
        analytics.track("llm_availability", mapOf("available" to isAvailable))
    }
}

suspend fun generateWithMetrics(prompt: String): LlmResponse? {
    val baseClient = LocalLlmClientFactory.create()
    val client = MetricsEnabledLlmClient(
        delegate = baseClient,
        reporter = MyMetricsReporter()
    )
    
    return try {
        client.generateText(LlmRequest(prompt))
    } catch (e: LlmError.ModelNotAvailable) {
        // Show user-friendly message
        showToast("AI features are not available on this device")
        null
    } catch (e: LlmError.SafetyBlocked) {
        showToast("Your request was blocked by content filters")
        null
    } catch (e: LlmError) {
        showToast("An error occurred: ${e.message}")
        null
    }
}
```

## Example 5: Feature Detection

```kotlin
fun checkFeatureSupport(client: LocalLlmClient) {
    println("Supported capabilities:")
    
    if (LlmCapability.TEXT_GENERATION in client.capabilities) {
        println("✓ Text Generation")
    }
    
    if (LlmCapability.SUMMARIZATION in client.capabilities) {
        println("✓ Summarization")
    }
    
    if (LlmCapability.REWRITING in client.capabilities) {
        println("✓ Rewriting")
    }
    
    if (LlmCapability.PROOFREADING in client.capabilities) {
        println("✓ Proofreading")
    }
    
    if (LlmCapability.STREAMING in client.capabilities) {
        println("✓ Streaming")
    }
}
```

## Example 6: Android Compose Integration

```kotlin
@Composable
fun LlmChatScreen() {
    val client = remember { LocalLlmClientFactory.create() }
    var userInput by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            label = { Text("Ask something...") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    try {
                        val result = client.generateText(
                            LlmRequest(prompt = userInput)
                        )
                        response = result.text
                    } catch (e: LlmError) {
                        response = "Error: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && userInput.isNotBlank()
        ) {
            Text(if (isLoading) "Generating..." else "Send")
        }
        
        if (response.isNotEmpty()) {
            Card(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = response,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
```

## Example 7: iOS SwiftUI Integration

```swift
import SwiftUI
import CoreLlm // KMP framework

struct LlmChatView: View {
    @State private var userInput = ""
    @State private var response = ""
    @State private var isLoading = false
    
    let client = LocalLlmClientFactory().create()
    
    var body: some View {
        VStack {
            TextField("Ask something...", text: $userInput)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding()
            
            Button(action: generate) {
                Text(isLoading ? "Generating..." : "Send")
            }
            .disabled(isLoading || userInput.isEmpty)
            
            if !response.isEmpty {
                Text(response)
                    .padding()
                    .background(Color.gray.opacity(0.1))
                    .cornerRadius(8)
            }
            
            Spacer()
        }
        .padding()
    }
    
    func generate() {
        isLoading = true
        
        let request = LlmRequest(
            prompt: userInput,
            systemInstruction: nil,
            maxTokens: 512,
            temperature: 0.7,
            metadata: [:]
        )
        
        Task {
            do {
                let result = try await client.generateText(request: request)
                await MainActor.run {
                    response = result.text
                    isLoading = false
                }
            } catch {
                await MainActor.run {
                    response = "Error: \(error.localizedDescription)"
                    isLoading = false
                }
            }
        }
    }
}
```
