# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a native Android 2048 puzzle game implementation written in Kotlin. The project features a complete implementation of the classic 2048 game with advanced features including undo functionality, AI hints, auto-save, and game statistics.

## Architecture

The project follows the MVVM (Model-View-ViewModel) architectural pattern:

- **Model**: Data classes and game logic (`model/` and `engine/`)
- **View**: Custom UI components and Android layouts (`view/` and `res/layout/`)
- **ViewModel**: Business logic and state management (`viewmodel/`)
- **Data**: Persistent storage using DataStore (`data/`)

### Key Components

- **GameEngine** (`engine/GameEngine.kt`): Core game logic including move operations, collision detection, undo system, and AI hint algorithm
- **GameView** (`view/GameView.kt`): Custom Canvas-based view for rendering the game grid with gesture detection and animations
- **GameViewModel** (`viewmodel/GameViewModel.kt`): Manages game state, coordinates between engine and UI, handles data persistence
- **MainActivity** (`MainActivity.kt`): Main UI controller with dialogs, settings, and user interactions
- **GamePreferences** (`data/GamePreferences.kt`): DataStore-based persistence for scores, statistics, and game state

## Build Commands

### Development Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug build to connected device
./gradlew installDebug

# Clean build artifacts
./gradlew clean

# Run tests
./gradlew test

# Run lint checks
./gradlew lint
```

### Environment Requirements
- Android Studio Hedgehog 2023.1.1 or higher
- JDK 17+ (configured via `gradle.properties`)
- Android SDK API 34
- Kotlin 1.9.0
- Gradle 8.5

## Game Features Implementation

### Core Game Logic (`GameEngine.kt`)
- **Grid Management**: 4x4 grid with tile positioning and collision detection
- **Move Algorithm**: Directional movement with tile merging logic
- **Undo System**: Maintains history of up to 5 game states using snapshot pattern
- **AI Hints**: Evaluates board positions using heuristics (empty cells, monotonicity, corner positioning)
- **Win/Loss Detection**: Automatic detection of game end conditions

### UI Implementation (`GameView.kt`)
- **Custom Canvas Drawing**: Manual tile rendering with dynamic colors and text sizing
- **Gesture Detection**: Swipe recognition for game controls
- **Animation System**: Tile appearance animations using ValueAnimator
- **Responsive Layout**: Automatic scaling based on screen dimensions

### Data Persistence (`GamePreferences.kt`)
- **DataStore Integration**: Modern Android preferences storage
- **Game Statistics**: Tracks total games, moves, average performance
- **Auto-save**: Automatic game state preservation
- **Settings Management**: Sound and vibration preferences

## Development Notes

### Adding New Features
- Game engine modifications should be made in `GameEngine.kt` with corresponding ViewModel updates
- UI changes require updates to both `GameView.kt` (for game area) and `activity_main.xml` (for controls)
- New data fields need to be added to both `GameState.kt` model and `GamePreferences.kt` persistence

### Testing Strategy
- Unit tests should focus on `GameEngine` logic (move operations, undo functionality, win conditions)
- UI tests should verify gesture recognition and animation behavior in `GameView`
- Integration tests should validate ViewModel coordination between components

### Performance Considerations
- The custom Canvas rendering in `GameView` is optimized for 60fps gameplay
- Coroutines are used extensively for non-blocking data operations
- Animation states are managed efficiently to prevent memory leaks

### Code Style
- Follows Android Kotlin style guide
- Uses view binding for type-safe view access
- Implements proper lifecycle management with ViewModels and LiveData
- Leverages Material Design 3 components for consistent UI

## Common Issues

### Build Problems
If encountering Java version issues, ensure `gradle.properties` points to correct JDK:
```properties
org.gradle.java.home=/Applications/Android Studio.app/Contents/jbr/Contents/Home
```

### Missing Resources
The project includes custom drawable resources for app icons. If resources are missing, they may need to be regenerated using Android Studio's Vector Asset tool.

### Performance Issues
If animations are choppy, check that hardware acceleration is enabled and consider reducing animation complexity in `GameView.kt`.