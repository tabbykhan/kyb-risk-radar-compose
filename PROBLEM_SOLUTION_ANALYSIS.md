# Problem-Solution Analysis: API Migration

## **Problem 1: Mock JSON Loading with Single-Use Asset Streams**

### Symptoms:
- App crashes with "AssetInputStream closed" errors
- JSON parsing failures intermittently
- "Failed to load KYB data" errors on some navigations
- Data loss on screen rotation or ViewModel recreation

### Root Cause:
The old `MockDataSource` loaded JSON from assets using `AssetManager.open("data.json")`. Asset input streams are **single-use** and cannot be reopened:

```kotlin
// ❌ PROBLEM: Reusing asset stream
assetManager.open("data.json").use { input →
    if (cachedJsonString != null) return cachedJsonString  // Early exit
    cachedJsonString = input.bufferedReader().readText()  // Stream already consumed
}
```

If the stream was accessed but cache was null, subsequent calls would fail because the stream cannot be reopened.

### Solution Implemented:
**Deleted MockDataSource dependency** and replaced with `KybRemoteDataSource` that:
- Reads data from HTTP API (not assets)
- No single-use stream issues
- Proper error handling with Result<T> type
- No caching issues

**Code:**
```kotlin
// ✅ SOLUTION: Real API call with proper error handling
class KybRemoteDataSource(baseUrl: String = "http://10.0.2.2:8080") {
    suspend fun runKybCheck(customerId: String, correlationId: String): Result<KybRunResult> =
        withContext(Dispatchers.IO) {
            try {
                val result = apiService.runKybCheck(customerId, correlationId)
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)  // Proper error handling
            }
        }
}
```

**Benefits:**
- ✅ No stream reuse issues
- ✅ Result<T> prevents null returns
- ✅ Proper exception handling
- ✅ Structured logging at API layer

---

## **Problem 2: API Called Multiple Times During Recomposition**

### Symptoms:
- Multiple API requests sent for single user action
- Duplicate data in database
- Network traffic spikes
- Race conditions on data updates

### Root Cause:
Old implementation:
1. API called in `init {}` block (ViewModel initialization)
2. Recompositions triggered ViewModel recreation
3. Navigation back and forward re-triggered ViewModel
4. No correlation-id to track duplicate requests

```kotlin
// ❌ PROBLEM: API called in init
init {
    loadInitialState()  // Could call getKybData() internally
}

// Recomposition → ViewModel recreation → init{} called again → API called again
```

### Solution Implemented:

**1. Move API call out of init{}:**
```kotlin
// ✅ SOLUTION: init only loads static data
init {
    loadInitialState()  // Only loads customers and recent checks, no API
}

// API ONLY called from explicit button handler
fun runKybCheck() {
    val result = repository.runKybCheck(customerId, correlationId)
}
```

**2. Safe navigation trigger with flag:**
```kotlin
// ✅ SOLUTION: Remembered flag prevents duplicate navigation
var navigationTriggered by rememberSaveable { mutableStateOf(false) }

LaunchedEffect(workflowState) {
    if (workflowState is WorkflowState.Completed && !navigationTriggered) {
        navigationTriggered = true  // Prevent re-trigger
        onNavigateToCustomerDetail(customerId, correlationId)
        viewModel.resetWorkflow()
        navigationTriggered = false
    }
}
```

**Benefits:**
- ✅ API called ONLY when user clicks button
- ✅ No duplicate requests on recomposition
- ✅ Safe navigation with one-time trigger
- ✅ Clear separation: init (static) vs. runKybCheck (dynamic)

---

## **Problem 3: No Correlation-ID Propagation**

### Symptoms:
- Cannot trace requests across client and server
- Debugging distributed issues difficult
- No link between logs and API calls
- Summary/Risk/Decision screens don't know request context

### Root Cause:
Old implementation had no mechanism to generate and pass correlation-id through the system:

```kotlin
// ❌ PROBLEM: No correlation-id
val kybData = repository.getKybData(customerId)  // No correlation-id parameter
```

### Solution Implemented:

**1. Generate in ViewModel:**
```kotlin
// ✅ SOLUTION: Generate and store in ViewModel
fun runKybCheck() {
    val correlationId = CorrelationIdProvider.generate()  // UUID
    _currentCorrelationId.value = correlationId
}
```

**2. Pass through Repository:**
```kotlin
// ✅ SOLUTION: Parameter in interface
interface KybRepository {
    suspend fun runKybCheck(customerId: String, correlationId: String): Result<KybRunResult>
}
```

**3. Add to HTTP Headers:**
```kotlin
// ✅ SOLUTION: Retrofit header injection
@GET("/kyb/mcp/run/{customerId}")
suspend fun runKybCheck(
    @Path("customerId") customerId: String,
    @Header("correlation-id") correlationId: String  // Automatic header
): KybRunResult
```

**4. Pass to Next Screen:**
```kotlin
// ✅ SOLUTION: Navigation includes correlationId
onNavigateToCustomerDetail(customerId, completedState.correlationId)

// Next screen receives it
fun CustomerDetailScreen(
    customerId: String,
    correlationId: String,  // ✅ Same ID
    summaryViewModel: SummaryViewModel,
    ...
)
```

**5. Structured Logging at All Layers:**
```kotlin
// ✅ SOLUTION: Correlation-id in all logs
Logger.logEvent(
    eventName = "KYB_RUN_STARTED",
    correlationId = correlationId,  // ✅ Traced
    customerId = customerId,
    screenName = "Dashboard"
)
```

**Benefits:**
- ✅ End-to-end request tracing
- ✅ Link client logs to server logs
- ✅ Context available in all screens
- ✅ Debugging and auditing capabilities

---

## **Problem 4: No Error Handling Contract**

### Symptoms:
- Null returns force ViewModel null checks
- Exceptions bypass error handling
- No clear error state for UI
- Data layer throws exceptions to UI layer

### Root Cause:
Old interface returned nullable type:

```kotlin
// ❌ PROBLEM: Nullable return, no error info
interface KybRepository {
    suspend fun getKybData(...): KybData?  // null could mean error or no data
}

// ViewModel must check
val data = repository.getKybData(...)
if (data != null) {
    // Success
} else {
    // Error? Or no data? Unknown
}
```

### Solution Implemented:

**1. Result<T> Type for Explicit Handling:**
```kotlin
// ✅ SOLUTION: Result type distinguishes success/failure
interface KybRepository {
    suspend fun runKybCheck(customerId: String, correlationId: String): Result<KybRunResult>
}

// Clear, explicit handling
val result = repository.runKybCheck(customerId, correlationId)
result.onSuccess { data → /* Handle success */ }
result.onFailure { error → /* Handle failure with message */ }
```

**2. Exception Handling at Data Layer:**
```kotlin
// ✅ SOLUTION: Data layer catches and returns Result
class KybRemoteDataSource {
    suspend fun runKybCheck(...): Result<KybRunResult> {
        return try {
            val result = apiService.runKybCheck(...)
            Result.success(result)
        } catch (e: Exception) {
            Logger.logError("API_KYB_RUN_FAILED", e, correlationId, customerId)
            Result.failure(e)  // Return error, don't throw
        }
    }
}
```

**3. Error Mapping to UiState:**
```kotlin
// ✅ SOLUTION: ViewModel converts Result to UiState
result.onFailure { error →
    _workflowState.value = WorkflowState.Error(error.message ?: "Failed")
}

// UI observes error state
when (workflowState) {
    is WorkflowState.Error → Text("Error: ${workflowState.message}")
}
```

**Benefits:**
- ✅ No null checks needed
- ✅ Clear success vs. failure path
- ✅ Error message available in UI
- ✅ No exceptions bypass layers
- ✅ Type system prevents bugs

---

## **Problem 5: Missing Dependency Injection Setup**

### Symptoms:
- Cannot switch between mock and real API
- Difficult to test (would need MockDataSource)
- Hard to configure API base URL
- Tight coupling to MockDataSource

### Root Cause:
AppModule hardcoded MockDataSource:

```kotlin
// ❌ PROBLEM: Hardcoded mock
fun provideKybRepository(context: Context): KybRepository {
    val mockDataSource = MockDataSource(context.assets)  // Hard-coded
    kybRepository = KybRepositoryImpl(mockDataSource, dataStoreManager)
}
```

### Solution Implemented:

**1. DI Provider for RemoteDataSource:**
```kotlin
// ✅ SOLUTION: Dedicated provider
private var remoteDataSource: KybRemoteDataSource? = null

fun provideRemoteDataSource(): KybRemoteDataSource {
    if (remoteDataSource == null) {
        remoteDataSource = KybRemoteDataSource()
    }
    return remoteDataSource!!
}
```

**2. Configurable Base URL:**
```kotlin
// ✅ SOLUTION: Constructor parameter with default
class KybRemoteDataSource(baseUrl: String = "http://10.0.2.2:8080") {
    // Configurable for testing, defaults to emulator localhost
}

// Can override for tests or different environment
fun provideRemoteDataSource(baseUrl: String): KybRemoteDataSource {
    return KybRemoteDataSource(baseUrl)
}
```

**3. Updated Repository Provider:**
```kotlin
// ✅ SOLUTION: Uses new remote source
fun provideKybRepository(context: Context): KybRepository {
    if (kybRepository == null) {
        val remote = provideRemoteDataSource()  // ✅ Real API
        val dataStore = provideDataStoreManager(context)
        kybRepository = KybRepositoryImpl(remote, dataStore)  // ✅ Injected
    }
    return kybRepository!!
}
```

**Benefits:**
- ✅ Easy to switch implementations
- ✅ Configurable for different environments
- ✅ Testable (can inject mock RemoteDataSource)
- ✅ Loose coupling between layers

---

## **Problem 6: No Workflow State Propagation**

### Symptoms:
- UI doesn't show workflow progress
- No "Fetching results..." message
- User doesn't know what's happening during API call
- No visual feedback between workflow steps and API call

### Root Cause:
Old implementation didn't connect workflow simulation to API state:

```kotlin
// ❌ PROBLEM: Workflow simulation not linked to API
_workflowState.value = WorkflowState.Running(...)
// ... simulate steps ...
val kybData = repository.getKybData(...)  // API call happens after UI built
```

### Solution Implemented:

**1. Workflow Sequence:**
```kotlin
// ✅ SOLUTION: Clear state progression
// 1. WorkflowState.Idle (initial)
//    ↓ User clicks button
// 2. WorkflowState.Running (workflow steps 1-4)
//    ↓ All steps complete
// 3. WorkflowState.FetchingResults (show "Fetching results...")
//    ↓ API response received
// 4. WorkflowState.Completed(correlationId, riskBand)
//    ↓ LaunchedEffect triggers navigation
```

**2. ViewModel Implementation:**
```kotlin
// ✅ SOLUTION: Step simulation then API call
fun runKybCheck() {
    // 1. Show workflow running
    _workflowState.value = WorkflowState.Running(emptyList())
    
    // 2. Simulate steps
    val steps = WorkflowStep.values().toList()
    steps.forEachIndexed { index, step →
        delay(2000)
        val completedSteps = steps.take(index + 1)
        _workflowState.value = if (index == steps.size - 1) {
            WorkflowState.FetchingResults(completedSteps)  // Show "Fetching..."
        } else {
            WorkflowState.Running(completedSteps)
        }
    }
    
    // 3. Then call API
    val result = repository.runKybCheck(customerId, correlationId)
    
    // 4. Update workflow state with result
    result.onSuccess { kybRunResult →
        _workflowState.value = WorkflowState.Completed(correlationId, riskBand)
    }
}
```

**3. UI Follows State:**
```kotlin
// ✅ SOLUTION: UI updates automatically
when (workflowState) {
    is WorkflowState.Running → {
        WorkflowStepIndicator(workflowState.completedSteps)
    }
    is WorkflowState.FetchingResults → {
        WorkflowStepIndicator(workflowState.completedSteps)
        Text("Fetching results...")  // Show during API call
    }
    is WorkflowState.Completed → {
        Text("Workflow completed successfully!")  // Show on success
        // LaunchedEffect triggers navigation here
    }
}
```

**Benefits:**
- ✅ User sees progress
- ✅ Clear state transitions
- ✅ "Fetching results..." message while API calls
- ✅ Workflow state drives navigation trigger
- ✅ Reactive UI with no manual event handling

---

## Summary Table

| Problem | Cause | Solution | Benefit |
|---------|-------|----------|---------|
| Asset stream reuse | Single-use InputStreams | Real API via RemoteDataSource | No stream issues |
| Multiple API calls | API in init {} | Move to runKybCheck() button handler | Single call per action |
| No request tracing | No correlation-id | Generate and propagate UUID | End-to-end tracing |
| No error contract | Nullable returns | Result<T> type | Type-safe error handling |
| No DI flexibility | Hardcoded MockDataSource | Provider pattern with RemoteDataSource | Testable, configurable |
| No workflow feedback | Disconnected state | Link workflow state to API state | User sees progress |

---

## Verification Checklist

- [x] **Problem 1:** Removed MockDataSource dependency
- [x] **Problem 2:** API call ONLY in runKybCheck() method (not init)
- [x] **Problem 3:** Correlation-id generated, passed through system, added to HTTP headers
- [x] **Problem 4:** Result<T> type replaces nullable returns
- [x] **Problem 5:** AppModule provides RemoteDataSource with DI pattern
- [x] **Problem 6:** WorkflowState progression linked to API lifecycle
- [x] **Navigation:** Safe trigger with rememberSaveable flag
- [x] **Logging:** Structured correlation-id at all layers
- [x] **Architecture:** MVVM + Clean Architecture maintained
- [x] **Coroutines:** Proper viewModelScope and Dispatchers.IO usage

All problems identified in the original requirements have been addressed and verified.
