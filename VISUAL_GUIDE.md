# Visual Implementation Guide

## System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                         USER INTERFACE LAYER                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │               DashboardScreen (Composable)                │  │
│  │  - Observes StateFlow<UiState<DashboardUiState>>         │  │
│  │  - Observes StateFlow<WorkflowState>                     │  │
│  │  - Safe navigation with rememberSaveable flag            │  │
│  │  - Calls viewModel.runKybCheck() on button click         │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
                            ↕ UI Events
┌────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER (MVVM)                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              DashboardViewModel                          │  │
│  │  - Manages UiState                                       │  │
│  │  - Manages WorkflowState                                 │  │
│  │  - Stores currentCorrelationId                           │  │
│  │  - runKybCheck() method: ONLY place API is called        │  │
│  │  - Handles success/failure from repository               │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
                        ↕ Business Logic
┌────────────────────────────────────────────────────────────────┐
│                       DOMAIN LAYER                              │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │            KybRepository (Interface)                     │  │
│  │  - runKybCheck(...): Result<KybRunResult>                │  │
│  └──────────────────────────────────────────────────────────┘  │
│                            ↑                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │          KybRepositoryImpl (Implementation)               │  │
│  │  - Logs: REPOSITORY_KYB_RUN_REQUESTED                    │  │
│  │  - Delegates to RemoteDataSource                         │  │
│  │  - Returns Result<KybRunResult>                          │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
                        ↕ Data Orchestration
┌────────────────────────────────────────────────────────────────┐
│                      DATA LAYER                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         KybRemoteDataSource                              │  │
│  │  - Configures Retrofit + OkHttp                          │  │
│  │  - Makes HTTP GET request                               │  │
│  │  - Adds correlation-id header                           │  │
│  │  - Returns Result<KybRunResult> (never throws)          │  │
│  │  - Logs: API_KYB_RUN_REQUEST, API_KYB_RUN_SUCCESS, etc. │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
                        ↕ HTTP Protocol
┌────────────────────────────────────────────────────────────────┐
│                     EXTERNAL SERVICES                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │            Backend API Server                            │  │
│  │  GET /kyb/mcp/run/{customerId}                          │  │
│  │  Header: correlation-id: <uuid>                         │  │
│  │  Response: KybRunResult JSON                            │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

---

## State Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                       WORKFLOW STATES                            │
└─────────────────────────────────────────────────────────────────┘

WorkflowState.Idle
  │ (No API call yet)
  │ Button visible and enabled
  │
  └──> [User clicks "Start Risk Scan" button]
       │
       ▼
WorkflowState.Running(emptyList())
  │ (Workflow started)
  │ Button hidden, loader shown
  │ Workflow card visible
  │ No steps completed yet
  │
  └──> [delay(2000), complete Step 1]
       │
       ▼
WorkflowState.Running([Step1])
  │ Step 1 turns green
  │ Steps 2-4 gray
  │
  └──> [delay(2000), complete Step 2]
       │
       ▼
WorkflowState.Running([Step1, Step2])
  │ Steps 1-2 green
  │ Steps 3-4 gray
  │
  └──> [delay(2000), complete Step 3]
       │
       ▼
WorkflowState.Running([Step1, Step2, Step3])
  │ Steps 1-3 green
  │ Step 4 gray
  │
  └──> [delay(2000), complete Step 4]
       │
       ▼
WorkflowState.FetchingResults([Step1, Step2, Step3, Step4])
  │ All steps green
  │ "Fetching results..." message shows
  │ [API call happens here]
  │
  └──> [API response received]
       │
       ▼
WorkflowState.Completed(correlationId, riskBand)
  │ All steps green
  │ "Workflow completed successfully!" shows
  │ LaunchedEffect detects this state
  │
  └──> [LaunchedEffect triggers navigation]
       │
       ▼
   [Navigate to CustomerDetailScreen]
   │
   ├──> viewModel.resetWorkflow()
   │
   └──> WorkflowState reverts to Idle
         │
         └──> User can Start Risk Scan again

┌─────────────────────────────────────────────────────────────────┐
│                    ERROR STATE BRANCH                            │
└─────────────────────────────────────────────────────────────────┘

WorkflowState.Running(...) → [API call fails]
  │
  ▼
WorkflowState.Error("Failed to load KYB data")
  │ Error message displayed
  │ User can click button again
  │
  └──> [User retries]
       │
       └──> Back to WorkflowState.Running
```

---

## Correlation-ID Propagation Chain

```
┌─────────────────────────────────────────────────────────────────┐
│                 CORRELATION-ID FLOW                              │
└─────────────────────────────────────────────────────────────────┘

1. DashboardViewModel.runKybCheck()
   └─ val correlationId = CorrelationIdProvider.generate()  [UUID]
   └─ _currentCorrelationId.value = correlationId           [Store]
   
2. repository.runKybCheck(customerId, correlationId)
   └─ Pass: customerId, correlationId
   
3. KybRepositoryImpl.runKybCheck()
   └─ Logger.logEvent(..., correlationId = correlationId)   [Log]
   └─ return remoteDataSource.runKybCheck(...)
   
4. KybRemoteDataSource.runKybCheck()
   └─ @Header("correlation-id") correlationId: String       [Header]
   └─ Logger.logEvent(..., correlationId = correlationId)   [Log]
   
5. HTTP Request
   └─ GET http://10.0.2.2:8080/kyb/mcp/run/CUST-0001
   └─ Header: correlation-id: f47ac10b-58cc-4372...       [Sent]
   
6. Backend API
   └─ Receives header: correlation-id: f47ac10b-58cc-4372...
   └─ Logs all events with same correlation-id
   └─ Response includes processed data
   
7. HTTP Response
   └─ 200 OK with KybRunResult JSON
   └─ Backend logs: [correlation-id=f47ac10b] Response sent
   
8. KybRemoteDataSource (Response)
   └─ Map JSON to KybRunResult
   └─ Logger.logEvent(..., correlationId = correlationId)   [Log]
   └─ return Result.success(kybRunResult)
   
9. KybRepositoryImpl (Response)
   └─ Passes through to ViewModel
   
10. DashboardViewModel
    └─ result.onSuccess { kybRunResult →
    └─   _workflowState.value = WorkflowState.Completed(
    └─     correlationId = correlationId,  [Same ID]
    └─     riskBand = kybRunResult.riskAssessment.riskBand
    └─   )
    
11. DashboardScreen
    └─ onNavigateToCustomerDetail(customerId, correlationId)  [Pass]
    
12. Navigation
    └─ CustomerDetailScreen(customerId, correlationId)  [Receive]
    
13. Detail Screens
    └─ SummaryViewModel(..., correlationId)
    └─ RiskActionsViewModel(..., correlationId)
    └─ DecisionViewModel(..., correlationId)
    
14. Subsequent API Calls
    └─ Can pass same correlationId for related requests
    └─ Backend can group related operations
```

---

## Lifecycle Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│              VIEWMODEL LIFECYCLE & API CALLS                    │
└─────────────────────────────────────────────────────────────────┘

1. DashboardScreen Created
   │
   └──> [Recomposition might happen]
        │
        └──> ViewModel NOT recreated
             │
             └──> init {} NOT called again
                  │
                  └──> loadInitialState() NOT called again

2. ViewModel Creation
   │
   └──> init {}
        │
        ├──> loadInitialState()
        │    │
        │    ├──> Load customers from repository    [NO API]
        │    │
        │    ├──> Load recent checks from database  [NO API]
        │    │
        │    └──> Update UiState.Success
        │
        └──> ✅ API NOT called in init

3. User Interaction
   │
   └──> [User clicks "Start Risk Scan" button]
        │
        └──> DashboardScreen calls viewModel.runKybCheck()
             │
             └──> ✅ API CALLED HERE (ONLY PLACE)
                  │
                  ├──> Generate correlationId
                  ├──> Show loader, hide button
                  ├──> Simulate workflow steps
                  ├──> Make API call
                  ├──> Update WorkflowState
                  │
                  └──> LaunchedEffect detects Completed
                       │
                       └──> Navigate to DetailScreen

4. Navigation
   │
   └──> [User goes to CustomerDetailScreen]
        │
        └──> DashboardViewModel NOT destroyed yet
             (retained in back stack)

5. Back Navigation
   │
   └──> [User presses back]
        │
        ├──> CustomerDetailScreen destroyed
        │
        ├──> Return to DashboardScreen
        │    │
        │    └──> ViewModel still alive (same instance)
        │         │
        │         └──> ❌ NO init {} called
        │         └──> ❌ NO API call
        │
        └──> Can Start Risk Scan again with new correlationId

6. Configuration Change (Rotation)
   │
   └──> [Device rotates]
        │
        ├──> ViewModel NOT destroyed (retained by system)
        │
        ├──> DashboardScreen recomposed
        │    │
        │    └──> ✅ UI driven from same ViewModel StateFlows
        │
        └──> ❌ NO init {} called
             ❌ NO API call

7. ViewModel Destruction
   │
   └──> [User leaves the app or DashboardScreen popped]
        │
        └──> ViewModel.onCleared() called
             │
             └──> viewModelScope.cancel()
                  │
                  └──> All running coroutines cancelled
                       (e.g., if API call was in flight)

┌─────────────────────────────────────────────────────────────────┐
│                    KEY TIMELINE                                  │
└─────────────────────────────────────────────────────────────────┘

T=0s:  ViewModel.init{}
T=1s:  User selects customer
T=2s:  User clicks "Start Risk Scan"
T=4s:  Workflow step 1 completes
T=6s:  Workflow step 2 completes
T=8s:  Workflow step 3 completes
T=10s: Workflow step 4 completes
T=11s: "Fetching results..." appears
       ↑ API CALL MADE (here, not earlier)
T=13s: API response received
T=13s: "Workflow completed successfully!" shows
T=14s: LaunchedEffect triggers navigation
T=15s: CustomerDetailScreen appears

```

---

## Data Model Relationships

```
┌──────────────────────────────────────────────────────────────────┐
│                    MODEL RELATIONSHIPS                            │
└──────────────────────────────────────────────────────────────────┘

┌────────────────────┐
│   KybRunResult     │  ← API Response Model
│  (from HTTP JSON)  │
├────────────────────┤
│ - auditTrail       │
│ - riskAssessment   │    ┌─────────────────────┐
│ - entityProfile    │────│ RiskAssessment      │
│ - partySummary     │    ├─────────────────────┤
│ - ... other fields │    │ - score: Int        │
└────────────────────┘    │ - riskBand: RiskBand│
                          └─────────────────────┘

     Used by ViewModel
           │
           ▼

┌────────────────────────┐
│  RecentKybCheck        │  ← Persisted Model
│ (saved in DataStore)   │
├────────────────────────┤
│ - customerId: String   │
│ - customerName: String │
│ - riskBand: RiskBand   │  ← From KybRunResult
│ - timestamp: Long      │
│ - correlationId: String│  ← From ViewModel
└────────────────────────┘

     Used by DashboardScreen
           │
           ▼

┌────────────────────────┐
│ DashboardUiState       │  ← UI State Model
├────────────────────────┤
│ - availableCustomers   │
│ - isFirstTime          │
│ - isRunButtonEnabled   │
│ - customerNameMap      │
└────────────────────────┘

     Observed as

┌────────────────────────┐
│ UiState<DashboardUI>   │  ← Sealed UI State
├────────────────────────┤
│ - Loading              │
│ - Success(data)        │
│ - Error(message)       │
└────────────────────────┘

     And

┌────────────────────────┐
│ WorkflowState          │  ← Workflow UI State
├────────────────────────┤
│ - Idle                 │
│ - Running(steps)       │
│ - FetchingResults(steps)
│ - Completed(id, band)  │  ← Contains correlationId
│ - Error(message)       │
└────────────────────────┘
```

---

## File Dependency Graph

```
┌──────────────────────────────────────────────────────────────────┐
│                    DEPENDENCY GRAPH                               │
└──────────────────────────────────────────────────────────────────┘

DashboardScreen.kt
    │
    ├─ imports DashboardViewModel
    │
    └─ calls viewModel.runKybCheck()
       │
       └─ calls viewModel.selectCustomer()

DashboardViewModel.kt
    │
    ├─ imports KybRepository
    │
    ├─ imports CorrelationIdProvider
    │
    ├─ imports Logger
    │
    └─ calls repository.runKybCheck()
           └─ calls repository.saveRecentCheck()
           └─ calls repository.getRecentChecks()

KybRepositoryImpl.kt
    │
    ├─ imports KybRemoteDataSource
    │
    ├─ imports DataStoreManager
    │
    └─ implements KybRepository
       └─ calls remoteDataSource.runKybCheck()

KybRemoteDataSource.kt
    │
    ├─ imports KybApiService
    │
    ├─ imports KybRunResult
    │
    ├─ imports RiskBandDeserializer
    │
    ├─ imports Logger
    │
    └─ calls apiService.runKybCheck()

KybApiService.kt (interface in RemoteDataSource.kt)
    │
    └─ annotation-based Retrofit interface
       └─ depends on Retrofit
       └─ depends on OkHttp

AppModule.kt
    │
    ├─ provides KybRemoteDataSource
    │
    ├─ provides KybRepositoryImpl
    │
    └─ provides DataStoreManager

KybRepository.kt (interface)
    │
    ├─ imports KybRunResult
    │
    └─ import RecentKybCheck

┌──────────────────────────────────────────────────────────────────┐
│                  EXTERNAL DEPENDENCIES                            │
└──────────────────────────────────────────────────────────────────┘

Retrofit 2.9.0
    ├─ HTTP client
    ├─ Annotation processing
    └─ REST API abstraction

OkHttp 4.11.0
    ├─ HTTP transport
    ├─ Logging interceptor
    └─ Connection management

Gson 2.10.1 (already present)
    ├─ JSON serialization
    ├─ RiskBandDeserializer
    └─ Object mapping

AndroidX (already present)
    ├─ Coroutines
    ├─ Lifecycle
    ├─ Compose
    └─ DataStore
```

---

## Testing Workflow

```
┌──────────────────────────────────────────────────────────────────┐
│                  TESTING EXECUTION FLOW                           │
└──────────────────────────────────────────────────────────────────┘

START
 │
 ├─ [Setup] Backend API running on localhost:8080
 │
 ├─ [Launch] App starts
 │  ├─ Dashboard loads (no API call)           ✅
 │  └─ Recent checks displayed from database   ✅
 │
 ├─ [Select] User selects customer
 │  ├─ Dropdown shows options                  ✅
 │  ├─ Selection stored in selectedCustomerId  ✅
 │  └─ Button becomes enabled                  ✅
 │
 ├─ [Click] User clicks "Start Risk Scan"
 │  ├─ Button hides immediately                ✅
 │  ├─ Loader appears                          ✅
 │  ├─ Workflow card becomes visible           ✅
 │  ├─ correlationId generated                 ✅
 │  └─ HTTP request prepared                   ✅
 │
 ├─ [Workflow] Simulate workflow steps
 │  ├─ Step 1: 2 seconds, turns green          ✅
 │  ├─ Step 2: 2 seconds, turns green          ✅
 │  ├─ Step 3: 2 seconds, turns green          ✅
 │  ├─ Step 4: 2 seconds, turns green          ✅
 │  └─ "Fetching results..." message shows     ✅
 │
 ├─ [API Call] HTTP request made
 │  ├─ URL: GET /kyb/mcp/run/CUST-0001         ✅
 │  ├─ Header: correlation-id: <uuid>          ✅
 │  ├─ Request logged                          ✅
 │  └─ Wait for response                       ✅
 │
 ├─ [Response] Backend returns data
 │  ├─ Status: 200 OK                          ✅
 │  ├─ Body: Valid KybRunResult JSON           ✅
 │  ├─ Response logged                         ✅
 │  └─ Parse to KybRunResult                   ✅
 │
 ├─ [Success] Handle successful response
 │  ├─ "Workflow completed successfully!" msg  ✅
 │  ├─ Recent check saved to database          ✅
 │  ├─ All workflow steps remain green         ✅
 │  ├─ WorkflowState.Completed emitted         ✅
 │  └─ correlationId passed                    ✅
 │
 ├─ [Navigation] LaunchedEffect triggers
 │  ├─ workflowState is Completed              ✅
 │  ├─ navigationTriggered is false            ✅
 │  ├─ onNavigateToCustomerDetail() called     ✅
 │  ├─ correlationId passed to detail screen   ✅
 │  └─ Navigation occurs                       ✅
 │
 ├─ [Detail Screen] CustomerDetailScreen shows
 │  ├─ Received customerId                     ✅
 │  ├─ Received correlationId                  ✅
 │  ├─ Summary/Risk/Decision screens can use ID ✅
 │  └─ Display KYB data                        ✅
 │
 ├─ [Back] User presses back
 │  ├─ Return to DashboardScreen               ✅
 │  ├─ ViewModel not destroyed                 ✅
 │  ├─ No extra API calls                      ✅
 │  └─ Can select and run again                ✅
 │
 ├─ [Retry] Click "Start Risk Scan" again
 │  ├─ New correlationId generated             ✅
 │  ├─ New API call made (one per action)      ✅
 │  └─ Workflow repeats                        ✅
 │
 └─ SUCCESS: All tests pass ✅
```

---

This visual guide provides quick reference for system architecture, state flows, data propagation, and testing procedures.
