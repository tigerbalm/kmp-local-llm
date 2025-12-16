import Foundation

#if canImport(LanguageModels)
import LanguageModels
#endif

/// Bridge between KMP and Apple Foundation Models API
/// Provides ObjC-compatible interface for Kotlin interop
@objc public class AppleLocalLlmBridge: NSObject {

    #if canImport(LanguageModels)
    @available(iOS 18.1, *)
    private var session: LanguageModelSession?
    #endif

    private let maxTokens: Int
    private let temperature: Double

    @objc public init(maxTokens: Int = 512, temperature: Double = 0.7) {
        self.maxTokens = maxTokens
        self.temperature = temperature
        super.init()
    }

    /// Check if Apple Intelligence and Foundation Models are available
    @objc public func isAvailable() -> Bool {
        #if canImport(LanguageModels)
        if #available(iOS 18.1, *) {
            // Check if LanguageModels framework is available
            // In practice, also check if Apple Intelligence is enabled
            return true
        }
        #endif
        return false
    }

    /// Prepare the LLM session
    @objc public func prepare(completion: @escaping (NSError?) -> Void) {
        #if canImport(LanguageModels)
        if #available(iOS 18.1, *) {
            Task {
                do {
                    // Initialize LanguageModelSession
                    self.session = try await LanguageModelSession()
                    DispatchQueue.main.async {
                        completion(nil)
                    }
                } catch {
                    DispatchQueue.main.async {
                        completion(error as NSError)
                    }
                }
            }
        } else {
            let error = NSError(
                domain: "AppleLocalLlmBridge",
                code: 1,
                userInfo: [NSLocalizedDescriptionKey: "iOS 18.1+ required for Apple Intelligence"]
            )
            completion(error)
        }
        #else
        let error = NSError(
            domain: "AppleLocalLlmBridge",
            code: 2,
            userInfo: [NSLocalizedDescriptionKey: "LanguageModels framework not available"]
        )
        completion(error)
        #endif
    }

    /// Generate text from prompt (non-streaming)
    @objc public func generate(
        prompt: String,
        systemInstruction: String?,
        completion: @escaping (String?, NSError?) -> Void
    ) {
        #if canImport(LanguageModels)
        if #available(iOS 18.1, *) {
            Task {
                do {
                    // Ensure session is prepared
                    if self.session == nil {
                        self.session = try await LanguageModelSession()
                    }

                    guard let session = self.session else {
                        throw NSError(
                            domain: "AppleLocalLlmBridge",
                            code: 3,
                            userInfo: [NSLocalizedDescriptionKey: "Session not initialized"]
                        )
                    }

                    // Build full prompt with system instruction
                    let fullPrompt: String
                    if let systemInstruction = systemInstruction {
                        fullPrompt = systemInstruction + "\n\n" + prompt
                    } else {
                        fullPrompt = prompt
                    }

                    // Generate response using Apple's Language Model
                    let response = try await session.generateText(from: fullPrompt)

                    DispatchQueue.main.async {
                        completion(response, nil)
                    }
                } catch {
                    DispatchQueue.main.async {
                        completion(nil, error as NSError)
                    }
                }
            }
        } else {
            let error = NSError(
                domain: "AppleLocalLlmBridge",
                code: 1,
                userInfo: [NSLocalizedDescriptionKey: "iOS 18.1+ required for Apple Intelligence"]
            )
            completion(nil, error)
        }
        #else
        let error = NSError(
            domain: "AppleLocalLlmBridge",
            code: 2,
            userInfo: [NSLocalizedDescriptionKey: "LanguageModels framework not available"]
        )
        completion(nil, error)
        #endif
    }

    /// Generate text with streaming (callback for each chunk)
    @objc public func generateStream(
        prompt: String,
        systemInstruction: String?,
        onChunk: @escaping (String) -> Void,
        onComplete: @escaping (NSError?) -> Void
    ) {
        #if canImport(LanguageModels)
        if #available(iOS 18.1, *) {
            Task {
                do {
                    // Ensure session is prepared
                    if self.session == nil {
                        self.session = try await LanguageModelSession()
                    }

                    guard let session = self.session else {
                        throw NSError(
                            domain: "AppleLocalLlmBridge",
                            code: 3,
                            userInfo: [NSLocalizedDescriptionKey: "Session not initialized"]
                        )
                    }

                    // Build full prompt with system instruction
                    let fullPrompt: String
                    if let systemInstruction = systemInstruction {
                        fullPrompt = systemInstruction + "\n\n" + prompt
                    } else {
                        fullPrompt = prompt
                    }

                    // Stream response using Apple's Language Model
                    for try await chunk in session.generateTextStream(from: fullPrompt) {
                        DispatchQueue.main.async {
                            onChunk(chunk)
                        }
                    }

                    DispatchQueue.main.async {
                        onComplete(nil)
                    }
                } catch {
                    DispatchQueue.main.async {
                        onComplete(error as NSError)
                    }
                }
            }
        } else {
            let error = NSError(
                domain: "AppleLocalLlmBridge",
                code: 1,
                userInfo: [NSLocalizedDescriptionKey: "iOS 18.1+ required for Apple Intelligence"]
            )
            onComplete(error)
        }
        #else
        let error = NSError(
            domain: "AppleLocalLlmBridge",
            code: 2,
            userInfo: [NSLocalizedDescriptionKey: "LanguageModels framework not available"]
        )
        onComplete(error)
        #endif
    }

    /// Cleanup and release resources
    @objc public func cleanup() {
        #if canImport(LanguageModels)
        if #available(iOS 18.1, *) {
            session = nil
        }
        #endif
    }
}
