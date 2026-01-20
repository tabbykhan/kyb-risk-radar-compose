# KYB Risk Radar - Android App

A Jetpack Compose Android application for KYB (Know Your Business) Early Risk Radar using MVVM architecture with clean architecture principles.

## Project Overview

This app provides a dashboard for running and monitoring KYB checks, viewing customer details, risk assessments, and managing RM (Relationship Manager) decisions.

## Architecture

The app follows **MVVM (Model-View-ViewModel)** pattern with **Clean Architecture** principles, organized in a single-module structure.

### Package Structure

```
com.nationwide.kyb
 ├── core
 │   ├── ui                    # Base UI components (BaseViewModel)
 │   ├── utils                 # Logger, CorrelationIdProvider, PdfUtils
 │   ├── navigation            # Navigation routes and setup
 │   └── di                    # Dependency injection (AppModule)
 ├── domain
 │   ├── model                 # Domain models (KybData, UiState, WorkflowStep)
 │   └── repository            # Repository interfaces
 ├── data
 │   ├── datasource            # MockDataSource (loads JSON from assets)
 │   ├── repository            # Repository implementations
 │   └── local                 # DataStoreManager (local persistence)
 └── feature
     ├── dashboard             # Dashboard screen and ViewModel
     ├── customerdetail        # Customer detail screen coordinator
     ├── summary               # Summary tab
     ├── riskactions           # Risk & Actions tab
     └── decision              # RM Decision and Audit tabs
```

### Key Components

#### Core Layer
- **Logger**: Structured logging with eventName, correlationId, customerId, screenName
- **CorrelationIdProvider**: Generates UUIDs for request tracing
- **PdfUtils**: Handles PDF copy from assets and opening with system viewer
- **BaseViewModel**: Base class with common error handling

#### Domain Layer
- **KybData**: Main domain model mapping JSON structure
- **UiState**: Sealed class for Loading/Success/Error states
- **WorkflowStep**: Enum for KYB workflow steps
- **RiskBand**: Enum for RED/AMBER/GREEN risk levels

#### Data Layer
- **MockDataSource**: Loads and parses JSON from assets/data.json
- **DataStoreManager**: Manages local persistence (selected customer, recent checks)
- **KybRepositoryImpl**: Implements repository interface with mock data

#### Feature Layer
- **DashboardViewModel**: Manages customer selection, workflow simulation, recent checks
- **SummaryViewModel**: Loads KYB data for summary tab
- **RiskActionsViewModel**: Loads KYB data for risk & actions tab
- **DecisionViewModel**: Manages RM override and comments
- **AuditViewModel**: Converts KYB data to JSON for audit tab

## Workflow

### First Time User Flow

1. **App Launch**
   - Customer dropdown shows hint: "Select a customer"
   - No default selection
   - "Start Risk Scan" button **DISABLED**
   - KYB workflow card inactive
   - Recent KYB checks list **EMPTY**

2. **Customer Selection**
   - User selects customer from dropdown
   - Selected customerId persisted in DataStore
   - "Run Ongoing KYB Check" button **ENABLED**
   - Selection restored on app restart

3. **KYB Workflow Simulation**
   - On "Run Ongoing KYB Check" click:
     - Generates correlationId (UUID)
     - Shows Analysis Timeline card
     - Animates steps every 2 seconds:
       1. Journey Classifier
       2. Entity & Parties
       3. Transactions Insights
       4. Companies House
       5. Risk & Rules
       6. KYB Summary Note
     - Each step turns GREEN sequentially
     - Shows loader + text "Fetching results..."

4. **After Workflow Completion**
   - Adds customer to Recent KYB Checks list
   - Shows risk badge color (RED / AMBER / GREEN) from mock JSON
   - Persists recent list locally
   - Navigates to Customer Detail Screen

### Customer Detail Screen

The Customer Detail screen contains 4 tabs:

#### Summary Tab
- Entity profile (customer details)
- Risk band & score
- KYB snapshot summary
- Supporting metrics
- Organization structure placeholder
- "Download KYB Copilot Report" CTA (copies PDF from assets and opens)

#### Risk & Actions Tab
- KYB notes
- Triggered rules with severity badges
- Recommended actions list

#### RM Decision Tab
- AI score display
- Key drivers
- RM override dropdown (RED / AMBER / GREEN)
- RM comments input
- Override state managed in ViewModel

#### Audit (JSON) Tab
- Raw JSON data from data.json file
- Pretty-printed for readability

## Observability

### Correlation ID Flow

Correlation IDs are generated and propagated throughout the app:

1. **Generation**: When "Run Ongoing KYB Check" is clicked, a UUID is generated
2. **Propagation**:
   - ViewModels store correlationId in state
   - Repository receives correlationId as parameter
   - Navigation passes correlationId as route argument
3. **Logging**: All structured logs include correlationId for tracing

### Structured Logging

The `Logger` utility provides structured logging with:
- `eventName`: Name of the event (e.g., "CUSTOMER_SELECTED", "KYB_RUN_STARTED")
- `correlationId`: Optional correlation ID for tracing
- `customerId`: Optional customer ID
- `screenName`: Optional screen name where event occurred
- `additionalData`: Optional key-value pairs for additional context

**Important**: No logging from Composables - only from ViewModels, Repositories, UseCases.

Example log events:
- `DASHBOARD_LOADED`
- `CUSTOMER_SELECTED`
- `KYB_RUN_STARTED`
- `WORKFLOW_STEP_COMPLETED`
- `KYB_RUN_COMPLETED`
- `NAVIGATION_CUSTOMER_DETAIL`
- `PDF_DOWNLOAD_REQUESTED`
- `RM_OVERRIDE_UPDATED`

## PDF Handling

The app handles PDFs through `PdfUtils`:

1. **Assets to Downloads/Cache**: Copies `kyb_copilot_report.pdf` from assets to downloads directory (or cache if unavailable)
2. **System Viewer**: Opens PDF using Android's system PDF viewer intent
3. **Extensible**: Prepared for future API-based PDF download

Location: `app/src/main/assets/kyb_copilot_report.pdf`

## Data Source

Mock data is loaded from:
- **Assets**: `app/src/main/assets/data.json`
- **Source**: `mock/data.json` (project root)

The JSON structure includes:
- Audit trail
- Transaction insights
- Risk assessment with triggers
- Entity profile
- Party summary
- Companies House data
- Sentiment analysis

## State Management

### UI State Pattern

All ViewModels use sealed `UiState` classes:
```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

### StateFlow

ViewModels expose state through `StateFlow`:
- `uiState: StateFlow<UiState<...>>`
- `selectedCustomerId: StateFlow<String?>`
- `recentChecks: StateFlow<List<RecentKybCheck>>`
- `workflowState: StateFlow<WorkflowState>`

## Local Persistence

DataStore is used for:
- **Selected Customer ID**: Persisted on selection, restored on app restart
- **Recent KYB Checks**: List of recent checks (max 10) with customer name, risk band, timestamp, correlation ID

## Navigation

Navigation Compose setup with routes:
- `dashboard` - Dashboard screen
- `customer_detail/{customerId}/{correlationId}` - Customer detail screen with tabs

Navigation handled in `KybNavigation` composable.

## Testing

### Unit Tests

Located in `app/src/test/java/com/nationwide/kyb/`:
- `DashboardViewModelTest`: Tests for dashboard functionality
  - Initial state loading
  - First-time user flow
  - Customer selection
  - Workflow state management

### Test Dependencies
- JUnit
- MockK (for mocking)
- Kotlin Coroutines Test

## Dependencies

Key dependencies:
- **Jetpack Compose**: UI framework
- **Material 3**: Design system
- **Navigation Compose**: Navigation
- **ViewModel Compose**: ViewModel support
- **DataStore Preferences**: Local persistence
- **Gson**: JSON parsing
- **Kotlin Coroutines**: Asynchronous operations

## Building and Running

1. Open project in Android Studio
2. Sync Gradle files
3. Ensure `data.json` exists in `app/src/main/assets/`
4. Run on device or emulator (minSdk 26)

## Future Enhancements

- API integration for real backend data
- API-based PDF download
- More comprehensive unit tests
- Integration tests
- UI tests with Compose Testing
- Enhanced error handling and retry logic
- Offline support with caching
- Background sync capabilities

## License

Internal use only - Nationwide KYB Risk Radar
