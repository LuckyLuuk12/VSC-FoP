# VSC-FoP (Feature-Oriented Programming for VS Code)

A VS Code extension to bring FeatureIDE's Feature-Oriented-Programming paradigm to a modern IDE.

## Overview

This extension provides Feature-Oriented Programming (FOP) capabilities in Visual Studio Code, enabling developers to work with feature models and build product variants.

## Features

- **Feature Tree View**: Visualize and manage feature models in the VS Code explorer with hierarchical structure showing:
  - Feature composition types (AND, OR, XOR/ALT)
  - Mandatory and abstract features
  - Visual icons and descriptions for each feature type
- **Auto-Load Model**: Automatically detects and loads `model.xml` files when opening a FOP project
- **Load Model Command**: Manually load FeatureIDE XML feature models from any location
- **Refresh Model Command**: Reload the current feature model (available in the Feature Model tree view toolbar)
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

**First time setup (Windows):**
```bash
cd java-backend
# Download Gradle wrapper jar
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar" -OutFile "gradle\wrapper\gradle-wrapper.jar"
# Build the project
.\gradlew.bat build
```

**Unix/Linux/Mac:**
```bash
cd java-backend
wget -O ./gradle/wrapper/gradle-wrapper.jar "https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar"
./gradlew build
```

## Installation

1. Clone this repository
2. Build both the TypeScript extension and Java backend
3. Open the project in VS Code
4. Press F5 to launch the extension in debug mode

## Usage

### Automatic Model Loading
The extension automatically detects FOP projects by looking for `model.xml` files in your workspace. When found, the feature model is automatically loaded and displayed in the Feature Model tree view.

### Manual Model Loading
1. Open the Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`)
2. Run `FOP: Load Model`
3. Select your FeatureIDE XML model file

### Refreshing the Model
Click the refresh icon in the Feature Model tree view toolbar or run `FOP: Refresh Model` from the Command Palette to reload the model.

### Feature Model Format
The extension supports FeatureIDE XML format with:
- **AND features**: All child features can be selected
- **OR features**: One or more child features must be selected
- **ALT (XOR) features**: Exactly one child feature must be selected
- **Mandatory features**: Must be included when parent is selected
- **Abstract features**: Grouping features without implementation

Example model structure:
```xml
<featureModel>
  <struct>
    <and mandatory="true" name="RootFeature">
      <alt abstract="true" mandatory="true" name="FeatureGroup">
        <feature name="Feature1"/>
        <feature name="Feature2"/>
      </alt>
      <or name="OptionalGroup">
        <feature name="OptionA"/>
        <feature name="OptionB"/>
      </or>
    </and>
  </struct>
</featureModel>
```

## Commands

- `FOP: Load Model` - Manually load a feature model from any location
- `FOP: Refresh Model` - Reload the feature model from workspace
- `FOP: Build Variant` - Build a product variant (implementation in progress)
