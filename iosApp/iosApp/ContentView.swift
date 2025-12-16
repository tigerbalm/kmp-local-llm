import SwiftUI
import SampleShared

struct ContentView: View {
    @StateObject private var viewModelWrapper = ChatViewModelWrapper()

    var body: some View {
        NavigationView {
            ChatView(viewModelWrapper: viewModelWrapper)
                .navigationTitle("KMP LLM Sample")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    if !viewModelWrapper.messages.isEmpty {
                        ToolbarItem(placement: .navigationBarTrailing) {
                            Button(action: {
                                viewModelWrapper.clearMessages()
                            }) {
                                Image(systemName: "trash")
                            }
                        }
                    }
                }
        }
    }
}

struct ChatView: View {
    @ObservedObject var viewModelWrapper: ChatViewModelWrapper
    @State private var inputText = ""
    @State private var useStreaming = true

    var body: some View {
        VStack(spacing: 0) {
            // Status Banner
            if !viewModelWrapper.isLlmAvailable {
                HStack {
                    Text("LLM not available on this device")
                        .font(.caption)
                        .foregroundColor(.white)
                        .padding()
                }
                .frame(maxWidth: .infinity)
                .background(Color.red)
            }

            // Error Banner
            if let error = viewModelWrapper.error {
                HStack {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.white)
                    Spacer()
                    Button("Dismiss") {
                        viewModelWrapper.dismissError()
                    }
                    .foregroundColor(.white)
                }
                .padding()
                .background(Color.red)
            }

            // Messages List
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 12) {
                        ForEach(viewModelWrapper.messages, id: \.id) { message in
                            MessageRow(message: message)
                        }

                        // Streaming message
                        if viewModelWrapper.isStreaming && !viewModelWrapper.currentStreamingText.isEmpty {
                            StreamingMessageRow(text: viewModelWrapper.currentStreamingText)
                        }
                    }
                    .padding()
                }
                .onChange(of: viewModelWrapper.messages.count) { _ in
                    if let lastMessage = viewModelWrapper.messages.last {
                        withAnimation {
                            proxy.scrollTo(lastMessage.id, anchor: .bottom)
                        }
                    }
                }
            }

            // Preset Prompts (when no messages)
            if viewModelWrapper.messages.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(PresetPromptType.allCases, id: \.self) { preset in
                            Button(preset.displayName) {
                                // Could be used with input
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.gray.opacity(0.2))
                            .cornerRadius(16)
                        }
                    }
                    .padding(.horizontal)
                }
                .padding(.vertical, 8)
            }

            Divider()

            // Input Area
            VStack(spacing: 8) {
                HStack(alignment: .bottom, spacing: 8) {
                    VStack(alignment: .leading) {
                        TextField("Ask something...", text: $inputText, axis: .vertical)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .lineLimit(1...4)
                            .disabled(!viewModelWrapper.isLlmAvailable || viewModelWrapper.isLoading || viewModelWrapper.isStreaming)

                        Toggle("Use streaming", isOn: $useStreaming)
                            .font(.caption)
                            .disabled(!viewModelWrapper.isLlmAvailable || viewModelWrapper.isLoading || viewModelWrapper.isStreaming)
                    }

                    Button(action: sendMessage) {
                        Image(systemName: "arrow.up.circle.fill")
                            .resizable()
                            .frame(width: 32, height: 32)
                    }
                    .disabled(inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
                             !viewModelWrapper.isLlmAvailable ||
                             viewModelWrapper.isLoading ||
                             viewModelWrapper.isStreaming)
                }
                .padding()
            }
        }
    }

    private func sendMessage() {
        let text = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }

        if useStreaming {
            viewModelWrapper.sendStreamingMessage(text: text)
        } else {
            viewModelWrapper.sendMessage(text: text)
        }
        inputText = ""
    }
}

struct MessageRow: View {
    let message: Message

    var body: some View {
        HStack {
            if message.isUser {
                Spacer()
            }

            VStack(alignment: message.isUser ? .trailing : .leading, spacing: 4) {
                Text(message.text)
                    .padding(12)
                    .background(message.isUser ? Color.blue.opacity(0.2) : Color.gray.opacity(0.2))
                    .cornerRadius(12)

                if !message.isUser {
                    if let tokenCount = message.tokenCount, let duration = message.durationMs {
                        Text("\(tokenCount) tokens • \(duration)ms")
                            .font(.caption2)
                            .foregroundColor(.gray)
                    } else if let tokenCount = message.tokenCount {
                        Text("\(tokenCount) tokens")
                            .font(.caption2)
                            .foregroundColor(.gray)
                    } else if let duration = message.durationMs {
                        Text("\(duration)ms")
                            .font(.caption2)
                            .foregroundColor(.gray)
                    }
                }
            }
            .frame(maxWidth: 250, alignment: message.isUser ? .trailing : .leading)

            if !message.isUser {
                Spacer()
            }
        }
    }
}

struct StreamingMessageRow: View {
    let text: String

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(text)
                    .padding(12)
                    .background(Color.gray.opacity(0.2))
                    .cornerRadius(12)

                ProgressView()
                    .scaleEffect(0.7)
            }
            .frame(maxWidth: 250, alignment: .leading)

            Spacer()
        }
    }
}

enum PresetPromptType: String, CaseIterable {
    case summarize = "Summarize"
    case rewrite = "Rewrite"
    case proofread = "Proofread"
    case explain = "Explain"

    var displayName: String {
        self.rawValue
    }
}

class ChatViewModelWrapper: ObservableObject {
    private let repository = LlmRepository()
    private var viewModel: ChatViewModel!

    @Published var messages: [Message] = []
    @Published var isLoading = false
    @Published var isLlmAvailable = false
    @Published var error: String? = nil
    @Published var currentStreamingText = ""
    @Published var isStreaming = false

    init() {
        // Create a scope for the viewModel
        let scope = createScope()
        viewModel = ChatViewModel(repository: repository, viewModelScope: scope)

        // Observe state changes
        viewModel.uiState.collect { [weak self] state in
            DispatchQueue.main.async {
                self?.messages = state.messages
                self?.isLoading = state.isLoading
                self?.isLlmAvailable = state.isLlmAvailable
                self?.error = state.error
                self?.currentStreamingText = state.currentStreamingText
                self?.isStreaming = state.isStreaming
            }
        } completionHandler: { error in
            if let error = error {
                print("Error collecting state: \(error)")
            }
        }
    }

    func sendMessage(text: String) {
        viewModel.sendMessage(text: text)
    }

    func sendStreamingMessage(text: String) {
        viewModel.sendStreamingMessage(text: text)
    }

    func clearMessages() {
        viewModel.clearMessages()
    }

    func dismissError() {
        viewModel.dismissError()
    }

    private func createScope() -> any Kotlinx_coroutines_coreCoroutineScope {
        // Create a MainScope for iOS
        return MainScope()
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
