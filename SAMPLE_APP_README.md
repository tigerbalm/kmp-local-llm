# KMP LLM Sample Applications

이 문서는 core_llm 라이브러리를 사용하는 Android 및 iOS 샘플 애플리케이션에 대한 설명입니다.

## 프로젝트 구조

```
kslm/
├── core_llm/              # KMP LLM 라이브러리
├── sampleShared/          # 공통 비즈니스 로직 (KMP)
│   ├── model/            # UI 상태 모델
│   ├── repository/       # LLM 클라이언트 래퍼
│   └── viewmodel/        # 공통 ViewModel
├── androidApp/           # Android 샘플 앱
│   └── ui/              # Jetpack Compose UI
└── iosApp/              # iOS 샘플 앱
    └── iosApp/          # SwiftUI UI
```

## 기능

### 주요 기능
- ✅ 텍스트 생성 (Text Generation)
- ✅ 스트리밍 응답 (Streaming Responses)
- ✅ 프리셋 프롬프트 (Summarize, Rewrite, Proofread, Explain)
- ✅ 메트릭스 표시 (토큰 수, 응답 시간)
- ✅ 에러 핸들링 및 사용자 피드백
- ✅ LLM 가용성 체크

### 공통 코드 (sampleShared)
- **ChatViewModel**: UI 상태 관리 및 비즈니스 로직
- **LlmRepository**: core_llm 클라이언트 래퍼
- **Models**: Message, ChatUiState, PresetPrompt

## Android 앱 빌드 및 실행

### 요구 사항
- Android Studio Hedgehog (2023.1.1) 이상
- Android SDK 34
- Kotlin 1.9+
- Gradle 8.0+

### 빌드 방법

1. **프로젝트 빌드**
   ```bash
   ./gradlew :androidApp:build
   ```

2. **Android Studio에서 실행**
   - Android Studio에서 프로젝트 열기
   - `androidApp` 모듈 선택
   - Run 버튼 클릭 또는 `Shift + F10`

3. **명령줄에서 설치**
   ```bash
   ./gradlew :androidApp:installDebug
   ```

### Android 특정 설정

#### AndroidManifest.xml
- 패키지: `com.thinq.kmp.sample.android`
- minSdk: 24
- targetSdk: 34

#### 필요한 의존성
- Jetpack Compose (Material 3)
- Kotlin Coroutines
- AndroidX Lifecycle

### Android 사전 준비사항
- **Gemini Nano 모델**: Android 기기에 Gemini Nano 모델이 다운로드되어 있어야 합니다
- **AICore**: Google Play Services의 AICore가 설치되어 있어야 합니다
- **최소 API 레벨**: 24 (Android 7.0)

## iOS 앱 빌드 및 실행

### 요구 사항
- Xcode 15.0 이상
- iOS 18.1 이상
- macOS Ventura (13.0) 이상
- CocoaPods 또는 Swift Package Manager

### 빌드 방법

1. **KMP Framework 빌드**
   ```bash
   ./gradlew :sampleShared:linkDebugFrameworkIosSimulatorArm64
   # 또는 실제 기기용
   ./gradlew :sampleShared:linkDebugFrameworkIosArm64
   ```

2. **Xcode 프로젝트 생성**
   - Xcode를 열고 "Create a new Xcode project" 선택
   - iOS > App 선택
   - Product Name: `iosApp`
   - Bundle Identifier: `com.thinq.kmp.sample.ios`
   - Interface: SwiftUI
   - Language: Swift

3. **Framework 링크**
   - Xcode 프로젝트에서 Target > Build Phases > Link Binary With Libraries
   - "+" 버튼 클릭하고 "Add Other..." > "Add Files..."
   - `sampleShared/build/bin/iosSimulatorArm64/debugFramework/SampleShared.framework` 추가
   - `core_llm/build/bin/iosSimulatorArm64/debugFramework/CoreLlm.framework` 추가

4. **Build Script 추가**
   - Build Phases > "+" > "New Run Script Phase"
   - 다음 스크립트 추가:
   ```bash
   cd "$SRCROOT/.."
   ./gradlew :sampleShared:embedAndSignAppleFrameworkForXcode
   ```

5. **파일 추가**
   - `iosApp/iosApp/` 디렉토리의 Swift 파일들을 Xcode 프로젝트에 추가
   - `iosApp.swift`
   - `ContentView.swift`
   - `Info.plist`

6. **실행**
   - Simulator 또는 실제 기기 선택
   - Run 버튼 클릭 또는 `Cmd + R`

### iOS 특정 설정

#### Info.plist
- Bundle Identifier: `com.thinq.kmp.sample.ios`
- Deployment Target: iOS 18.1
- Supported Interface Orientations 설정

#### Framework Search Paths
Xcode의 Build Settings에서:
- Framework Search Paths: `$(SRCROOT)/../sampleShared/build/bin/$(PLATFORM_NAME)/debugFramework`

### iOS 사전 준비사항
- **iOS 18.1+**: Apple Intelligence 기능 사용을 위해 필요
- **Apple Intelligence 활성화**: 설정 > Apple Intelligence & Siri에서 활성화
- **실제 기기 권장**: Simulator에서는 LLM 기능이 제한될 수 있음

## 사용 방법

### 기본 사용법

1. **앱 실행**
   - Android 또는 iOS 앱 실행
   - LLM 가용성 자동 체크

2. **메시지 전송**
   - 하단 입력창에 텍스트 입력
   - "Use streaming" 체크박스로 스트리밍 모드 선택/해제
   - 전송 버튼 클릭

3. **프리셋 프롬프트 사용**
   - 메시지가 없을 때 하단에 프리셋 버튼 표시
   - Summarize, Rewrite, Proofread, Explain 선택 가능

4. **메트릭스 확인**
   - AI 응답 아래에 토큰 수와 응답 시간 표시
   - 성능 모니터링 가능

### UI 구성

#### Android (Jetpack Compose)
- **TopBar**: 앱 제목, Clear 버튼
- **Status Banner**: LLM 가용성 상태
- **Error Banner**: 에러 메시지 및 Dismiss 버튼
- **Messages List**: 사용자/AI 메시지 목록
- **Preset Prompts**: 빠른 액션 버튼
- **Input Area**: 텍스트 입력, 스트리밍 옵션, 전송 버튼

#### iOS (SwiftUI)
- **NavigationBar**: 앱 제목, Trash 버튼
- **Status Banner**: LLM 가용성 상태
- **Error Banner**: 에러 메시지 및 Dismiss 버튼
- **ScrollView**: 사용자/AI 메시지 목록
- **Preset Prompts**: 수평 스크롤 버튼
- **Input Area**: 텍스트 입력, 스트리밍 토글, 전송 버튼

## 아키텍처

### 계층 구조

```
┌─────────────────────────────────────┐
│     UI Layer (Platform Specific)    │
│   Android (Compose) | iOS (SwiftUI) │
├─────────────────────────────────────┤
│    ChatViewModel (Shared)           │
│    - State Management               │
│    - Business Logic                 │
├─────────────────────────────────────┤
│    LlmRepository (Shared)           │
│    - Client Wrapper                 │
│    - Error Handling                 │
├─────────────────────────────────────┤
│    core_llm (Library)               │
│    - LocalLlmClient                 │
│    - Platform Implementations       │
└─────────────────────────────────────┘
```

### 데이터 흐름

1. **사용자 입력** → UI Layer
2. **UI Layer** → ChatViewModel.sendMessage()
3. **ChatViewModel** → LlmRepository.generateText()
4. **LlmRepository** → LocalLlmClient (core_llm)
5. **Platform LLM** → Response
6. **Response** → Repository → ViewModel → UI
7. **UI 업데이트** (StateFlow/ObservableObject)

## 트러블슈팅

### Android

#### 문제: "LLM is not available on this device"
**해결방법**:
- Google Play Services 업데이트
- AICore 설치 확인
- Gemini Nano 모델 다운로드
- 기기 호환성 확인 (최소 API 24)

#### 문제: 빌드 에러
**해결방법**:
```bash
./gradlew clean
./gradlew :androidApp:build --refresh-dependencies
```

### iOS

#### 문제: "LLM is not available on this device"
**해결방법**:
- iOS 18.1+ 확인
- 설정 > Apple Intelligence & Siri 활성화
- 실제 기기에서 테스트 (Simulator 제한 있음)

#### 문제: Framework not found
**해결방법**:
```bash
# Framework 재빌드
./gradlew :sampleShared:clean
./gradlew :sampleShared:linkDebugFrameworkIosSimulatorArm64
```

#### 문제: Xcode 빌드 에러
**해결방법**:
- Framework Search Paths 확인
- Build Script 실행 순서 확인
- Clean Build Folder (Cmd + Shift + K)

## 확장 및 커스터마이징

### 새로운 프리셋 추가

`sampleShared/src/commonMain/kotlin/com/thinq/kmp/sample/model/PresetPrompt.kt`:
```kotlin
enum class PresetPrompt(val displayName: String, val promptTemplate: String) {
    // 기존 프리셋...

    TRANSLATE(
        "Translate",
        "Translate the following text to Korean:\n\n"
    )
}
```

### UI 커스터마이징

#### Android
`androidApp/src/main/kotlin/com/thinq/kmp/sample/ui/ChatScreen.kt`에서 Compose 컴포넌트 수정

#### iOS
`iosApp/iosApp/ContentView.swift`에서 SwiftUI 뷰 수정

### 메트릭스 리포터 커스터마이징

`LlmRepository.kt`:
```kotlin
// ConsoleMetricsReporter 대신 커스텀 리포터 사용
private val client: LocalLlmClient = MetricsEnabledLlmClient(
    delegate = baseClient,
    reporter = CustomMetricsReporter() // 사용자 정의 리포터
)
```

## 라이선스

Copyright © 2024 ThinQ

## 참고 자료

- [core_llm 라이브러리 README](../README.md)
- [샘플 사용 예제](../SAMPLE_USAGE.md)
- [Kotlin Multiplatform 공식 문서](https://kotlinlang.org/docs/multiplatform.html)
- [Jetpack Compose 공식 문서](https://developer.android.com/jetpack/compose)
- [SwiftUI 공식 문서](https://developer.apple.com/xcode/swiftui/)
