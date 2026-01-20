# Quick Reference: API Implementation Changes

## Files Modified

| File | Purpose | Change Type |
|------|---------|-------------|
| **KybRemoteDataSource.kt** | API HTTP client | **NEW** |
| **KybRunResult.kt** | API response model | **NEW** |
| **KybRepository.kt** | Repository interface | Updated |
| **KybRepositoryImpl.kt** | Repository implementation | Updated |
| **AppModule.kt** | Dependency injection | Updated |
| **DashboardViewModel.kt** | State management | Updated |
| **DashboardScreen.kt** | UI navigation | Updated |
| **libs.versions.toml** | Dependencies | Updated |
| **build.gradle.kts** | Dependencies | Updated |

---

## Key API Changes

### Old Interface
```kotlin
suspend fun getKybData(customerId: String, correlationId: String): KybData?
```

### New Interface
```kotlin
suspend fun runKybCheck(customerId: String, correlationId: String): Result<KybRunResult>
```

---

## API Endpoint

### Configuration
```
Base URL: http://10.0.2.2:8080  (Android emulator localhost)
Endpoint: GET /kyb/mcp/run/{customerId}
Header:   correlation-id: <uuid>
```

### Example Request
```
GET http://10.0.2.2:8080/kyb/mcp/run/CUST-0001
Header: correlation-id: f47ac10b-58cc-4372-a567-0e02b2c3d479
```

### Expected Response
```json
{
  "riskAssessment": {
    "riskBand": "RED|AMBER|GREEN",
    "score": 85
  },
  "entityProfile": {
    "legalName": "Company Name"
  }
  // ... other fields
}
```

---

## Data Flow in 10 Steps

```
1. User clicks "Start Risk Scan" button
   └─ Triggers: viewModel.runKybCheck()

2. ViewModel generates UUID correlationId
   └─ Stores: _currentCorrelationId.value = correlationId

3. UI updates to show loader and workflow
   └─ Updates: _isRunButtonHidden.value = true
              _workflowState.value = WorkflowState.Running(...)

4. Workflow steps simulate (2 seconds each)
   └─ Updates: _workflowState with completed steps
   └─ Shows: "Fetching results..." when all steps done

5. Repository.runKybCheck() called
   └─ Pass: customerId, correlationId
   └─ Return: Result<KybRunResult>

6. RemoteDataSource.runKybCheck() executes
   └─ HTTP GET /kyb/mcp/run/{customerId}
   └─ Header: correlation-id: <uuid>
   └─ Maps: JSON → KybRunResult

7. ViewModel receives Result<KybRunResult>
   └─ Checks: result.onSuccess {} or result.onFailure {}

8. On success: Save recent check to DataStore
   └─ Updates: _workflowState = WorkflowState.Completed
   └─ Contains: correlationId for next screen

9. DashboardScreen observes WorkflowState.Completed
   └─ LaunchedEffect triggers
   └─ Navigation flag ensures single navigation

10. Navigate to CustomerDetailScreen
    └─ Pass: customerId, correlationId
    └─ Same correlationId available in all screens
```

---

## Error Handling

### Success Path
```kotlin
val result = repository.runKybCheck(customerId, correlationId)

result.onSuccess { kybRunResult ->
    // Process successful response
    val recentCheck = RecentKybCheck(
        customerId = customerId,
        customerName = kybRunResult.entityProfile.legalName,
        riskBand = kybRunResult.riskAssessment.riskBand,
        timestamp = System.currentTimeMillis(),
        correlationId = correlationId
    )
    repository.saveRecentCheck(recentCheck)
    
    _workflowState.value = WorkflowState.Completed(
        correlationId = correlationId,
        riskBand = kybRunResult.riskAssessment.riskBand
    )
}
```

### Failure Path
```kotlin
result.onFailure { exception ->
    Logger.logError(
        eventName = "KYB_RUN_FAILED",
        error = exception,
        correlationId = correlationId,
        customerId = customerId
    )
    
    _workflowState.value = WorkflowState.Error(
        exception.message ?: "Failed to load KYB data"
    )
}
```

---

## Correlation-ID Trace

```
Generate in ViewModel:
  correlationId = CorrelationIdProvider.generate()
  
Pass to Repository:
  repository.runKybCheck(customerId, correlationId)
  
Add to HTTP Header:
  Header: correlation-id: <uuid>
  
Log at all levels:
  Logger.logEvent(..., correlationId = correlationId)
  
Pass to next screen:
  onNavigateToCustomerDetail(customerId, correlationId)
  
Available in Detail screen:
  fun CustomerDetailScreen(..., correlationId: String)
  
Store in ViewModel/StateFlow:
  val _correlationId = MutableStateFlow(correlationId)
```

---

## Navigation Safety

### The Problem
```kotlin
// ❌ Without flag: LaunchedEffect triggers on every recomposition
LaunchedEffect(workflowState) {
    if (workflowState is WorkflowState.Completed) {
        onNavigateToCustomerDetail(...)  // Navigates multiple times!
    }
}
```

### The Solution
```kotlin
// ✅ With flag: Navigation happens once only
var navigationTriggered by rememberSaveable { mutableStateOf(false) }

LaunchedEffect(workflowState) {
    if (workflowState is WorkflowState.Completed && !navigationTriggered) {
        navigationTriggered = true  // Block further navigation
        onNavigateToCustomerDetail(...)  // Navigates once
        viewModel.resetWorkflow()
        navigationTriggered = false  // Reset for future
    }
}
```

---

## Workflow States

```
WorkflowState.Idle
└─ Initial state after DashboardScreen loads
└─ Button is visible and enabled

WorkflowState.Running(completedSteps: List<WorkflowStep>)
└─ User clicked button
└─ Showing workflow progress
└─ Steps turning green as completed
└─ Button is hidden

WorkflowState.FetchingResults(completedSteps: List<WorkflowStep>)
└─ All workflow steps completed
└─ API call in progress
└─ Showing "Fetching results..." message

WorkflowState.Completed(correlationId: String, riskBand: RiskBand)
└─ API response received successfully
└─ Showing completion message
└─ LaunchedEffect triggers navigation

WorkflowState.Error(message: String)
└─ API call failed
└─ Showing error message
└─ User can try again
└─ Button reappears after resetWorkflow()
```

---

## Testing Scenario

### Pre-Test Setup
```
1. Start backend API: http://localhost:8080
2. Ensure endpoint available: GET /kyb/mcp/run/{customerId}
3. Endpoint expects header: correlation-id: <uuid>
4. Backend returns valid KybRunResult JSON
```

### Test Steps
```
1. Launch app
   → Dashboard loads WITHOUT API call ✅
   
2. Select customer from dropdown
   → Button enables ✅
   
3. Click "Run Ongoing KYB Check"
   → Button hides immediately ✅
   → Loader appears ✅
   → Workflow card becomes visible ✅
   
4. Watch workflow steps
   → First step completes (2 seconds) ✅
   → Second step completes (2 more seconds) ✅
   → ... all steps complete ...
   → "Fetching results..." appears ✅
   
5. Check HTTP request
   → API called ONCE ✅
   → Header includes correlation-id ✅
   → Request body has correct customerId ✅
   
6. Verify response handling
   → If success: "Workflow completed successfully!" ✅
   → Recent check saved to list ✅
   → Navigation to CustomerDetailScreen ✅
   → correlationId passed to next screen ✅
   
7. Go back to Dashboard
   → No API call on back navigation ✅
   → Can Start Risk Scan again ✅
   → New correlationId generated ✅
```

---

## Common Issues & Solutions

### Issue: "Failed to load KYB data"
**Cause:** API endpoint not running or unreachable
**Solution:** 
- Verify backend running on localhost:8080
- Check endpoint: GET /kyb/mcp/run/{customerId}
- Use correct emulator IP: 10.0.2.2

### Issue: API called multiple times
**Cause:** Recomposition triggering multiple calls
**Solution:**
- Already fixed in new implementation
- API ONLY called from runKybCheck() method
- Never called from init {} or during recomposition

### Issue: correlationId null in next screen
**Cause:** Navigation not passing correlationId
**Solution:**
- Ensure onNavigateToCustomerDetail(customerId, correlationId)
- Both parameters must be in navigation route
- Receive both in CustomerDetailScreen

### Issue: Navigation not triggering
**Cause:** WorkflowState.Completed not reached
**Solution:**
- Verify API returns successful response
- Check result.onSuccess block is executing
- Verify _workflowState.value = WorkflowState.Completed is called
- Confirm navigationTriggered flag logic

---

## Dependencies Added

```gradle
// Retrofit - HTTP client and API interface
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// OkHttp - HTTP implementation with interceptors
implementation("com.squareup.okhttp3:okhttp:4.11.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
```

---

## Important Notes

1. **Never call API in init {}** - Only in response to user action
2. **Always use Result<T>** - Never return nullable data
3. **Pass correlationId everywhere** - For end-to-end tracing
4. **Use rememberSaveable for navigation flags** - Persists across recompositions
5. **Log at data layer** - Before returning Result
6. **Use viewModelScope** - For proper lifecycle management
7. **Never throw from data layer** - Always return Result

---

## Production Checklist

- [ ] Backend API deployed and tested
- [ ] Base URL configured correctly (not hardcoded localhost)
- [ ] correlation-id header expected by backend
- [ ] API returns proper KybRunResult JSON structure
- [ ] Error responses handled gracefully
- [ ] Network timeout configured appropriately
- [ ] Logging configured for production
- [ ] No sensitive data logged
- [ ] API calls can be throttled if needed
- [ ] Recent checks persist across app restarts
- [ ] No infinite loops in state transitions
- [ ] Navigation properly guarded against duplicates

---

## Architecture Summary

```
┌─────────────────────────────────────────────┐
│         UI Layer (Composable)               │
│  - DashboardScreen                          │
│  - Observes StateFlow                       │
│  - Drives from UiState & WorkflowState      │
│  - Safe navigation with LaunchedEffect      │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│      Presentation Layer (ViewModel)         │
│  - DashboardViewModel                       │
│  - Manages state with StateFlow              │
│  - Calls repository on user action          │
│  - Propagates correlationId                 │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│      Domain Layer (Repository)              │
│  - KybRepository interface                  │
│  - KybRepositoryImpl implementation          │
│  - Logs events                              │
│  - Delegates to data sources                │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│      Data Layer (Data Sources)              │
│  - KybRemoteDataSource (API calls)          │
│  - DataStoreManager (Local persistence)     │
│  - Handles HTTP + local storage             │
│  - Returns Result<T> (no exceptions)        │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│    External Services                        │
│  - Retrofit/OkHttp (HTTP)                   │
│  - DataStore (Persistence)                  │
│  - Backend API                              │
└─────────────────────────────────────────────┘
```

---

This quick reference covers all essential changes for understanding and maintaining the API integration.
