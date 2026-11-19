---
name: android-kotlin-clean-architecture
description: Production-grade Android & Kotlin guidelines based on Google NowInAndroid and top community standards. Covers Jetpack Compose performance, MVVM/MVI, Coroutines & Flow concurrency, Dagger Hilt, Offline-first Room architecture, and Material 3 design.
---

# Android & Kotlin Production Standards (Clean Architecture & Jetpack Compose)

This skill provides comprehensive, battle-tested standards distilled from Google's official Android Architecture guidelines (`NowInAndroid`) and top-tier community best practices (`cursor.directory`).

---

## 1. Architectural Layers & Separation of Concerns

Follow **Unidirectional Data Flow (UDF)** with strictly separated layers:

### Presentation Layer (UI & ViewModel)
- **Framework:** Jetpack Compose (Material 3). Do not use legacy XML layouts or ViewBinding.
- **State Management:**
  - ViewModels hold state in private `MutableStateFlow` and expose read-only immutable `StateFlow`.
  - Expose a single unified `UiState` per screen (prefer `sealed interface` over loose primitives):
    ```kotlin
    sealed interface TransactionUiState {
        data object Loading : TransactionUiState
        data class Success(val transactions: List<Transaction>) : TransactionUiState
        data class Error(val message: String) : TransactionUiState
    }
    ```
  - In Composables, always collect states using lifecycle awareness:
    ```kotlin
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ```
- **Events / Side Effects:**
  - Send UI events to ViewModel via simple function calls (e.g., `viewModel.onDeleteClicked(id)`).
  - For one-shot events (Navigate, Snackbar, Toast), use a `Channel` or `SharedFlow` with `LaunchedEffect`.

### Domain Layer (UseCases / Interactors)
- Keep UseCases single-responsibility with `operator fun invoke(...)`.
- UseCases must contain pure business logic and remain free of Android framework dependencies (no `Context`, `Toast`, or UI references).
- Return Kotlin's built-in `Result<T>` for one-shot operations, or `Flow<T>` for reactive continuous streams.

### Data Layer (Repositories & Data Sources)
- **Repository Pattern:** Abstract all data access behind domain interfaces (`TransactionRepository`).
- **Offline-First:** Room Database is the single source of truth for UI streams (`Flow<List<Entity>>`). Remote sync (Firebase/REST API) runs in the background and writes to Room.
- **Coroutines & Threading:**
  - Execute database operations and network calls on `Dispatchers.IO` using `withContext(Dispatchers.IO)`.
  - Suspend functions must be **main-safe** (safe to call directly from the main thread).

---

## 2. Jetpack Compose Performance & Recomposition Rules

### State Hoisting & Pure Composables
- Pass data down, pass events up (`state: StateType, onAction: () -> Unit`).
- Do not pass entire `ViewModel` instances deep down into child/leaf composables; pass only the required state slices and event lambdas.

### Stability & Avoiding Unnecessary Recomposition
- Use `@Immutable` or `@Stable` on custom UI models when they contain `List`, `Set`, or third-party classes.
- Use `remember` for expensive calculations:
  ```kotlin
  val formattedDate = remember(timestamp) { formatDate(timestamp) }
  ```
- Use `derivedStateOf` when observing high-frequency state changes (e.g., scroll offsets) that map to a boolean or discrete state:
  ```kotlin
  val showScrollToTop by remember {
      derivedStateOf { listState.firstVisibleItemIndex > 5 }
  }
  ```
- Use `key()` inside LazyColumn/LazyRow items:
  ```kotlin
  items(items = transactions, key = { it.id }) { item -> ... }
  ```

### Modifier Rules
- Every reusable Composable must accept an optional `modifier: Modifier = Modifier` as its first optional parameter and chain it to the root layout element.
- Chain modifiers in the correct visual order (`size` -> `padding` -> `background` -> `clip` -> `clickable`).

---

## 3. Kotlin & Coroutines Best Practices

- **Structured Concurrency:**
  - Never launch coroutines using `GlobalScope`.
  - In ViewModels, strictly use `viewModelScope.launch`.
  - For parallel work, use `coroutineScope` and `async/await` rather than uncontrolled thread spawns.
- **Exception Handling:**
  - Catch specific exceptions (`IOException`, `HttpException`, `FirebaseFirestoreException`) rather than swallowing generic `Throwable`.
  - Wrap API/Network calls with `runCatching` or `try/catch` and emit `Result.failure(e)` or UI `Error` states. Never let background exceptions crash the app.
- **Flow Patterns:**
  - Use `StateFlow` for state (`stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InitialState)`).
  - Use `callbackFlow` to wrap listener-based APIs (like Firebase `addSnapshotListener`) and ensure `awaitClose { listener.remove() }` is called to prevent memory leaks.

---

## 4. Dependency Injection (Dagger Hilt)

- Annotate ViewModels with `@HiltViewModel` and `@Inject constructor(...)`.
- Group module bindings cleanly:
  - `@InstallIn(SingletonComponent::class)` for app-wide singletons (Database, Network, Repositories).
  - Avoid constructor injection cycles; use interfaces to decouple implementations.

---

## 5. Security & Resource Guidelines

- **Bilingual & Localization:** Never hardcode user-facing strings in UI code; always use `stringResource(R.string.*)` with English (`values/strings.xml`) and Vietnamese (`values-vi/strings.xml`) support.
- **Sensitive Data:** Keep API keys, tokens, and secrets in `local.properties` (or environment variables) and expose them safely through `BuildConfig`. Never commit secrets to Git.
- **App Security:** For financial and banking apps, ensure secure authentication, session timeouts, and biometric verification (`BiometricPrompt`) on resume.
