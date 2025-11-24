# VSC-FoP (Feature-Oriented Programming for VS Code)

A VS Code extension to bring FeatureIDE's Feature-Oriented-Programming paradigm to a modern IDE.

## Overview

This extension provides Feature-Oriented Programming (FOP) capabilities in Visual Studio Code, enabling developers to work with feature models and build product variants.

## Features

- **Feature Tree View**: Visualize and manage feature models in the VS Code explorer
- **Load Model Command**: Load feature models from your project
- **Build Variant Command**: Build specific product variants based on feature configurations

## Architecture

The extension consists of two main components:

### TypeScript Extension (VSCode Frontend)
- `src/extension.ts` - Extension entry point and command registration
- `src/FeatureTreeProvider.ts` - Tree view data provider for feature visualization
- `src/FeatureItem.ts` - Tree item model for features
- `src/JavaBridge.ts` - Bridge to communicate with Java backend

### Java Backend
- `java-backend/src/main/java/com/fop/backend/BackendMain.java` - Main entry point
- `java-backend/src/main/java/com/fop/backend/ModelLoader.java` - Feature model loading
- `java-backend/src/main/java/com/fop/backend/FeatureHouseInvoker.java` - Feature composition

## Building

### TypeScript Extension
```bash
npm install
npm run compile
```

### Java Backend
```bash
cd java-backend
./gradlew build
```

## Installation

1. Clone this repository
2. Build both the TypeScript extension and Java backend
3. Open the project in VS Code
4. Press F5 to launch the extension in debug mode

## Commands

- `FOP: Load Model` - Load a feature model
- `FOP: Build Variant` - Build a product variant

## Development Status

This is a scaffolded version with minimal implementation. The structure and classes are in place but contain no business logic.
