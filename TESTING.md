# Testing the FOP Extension

## Quick Test Setup

1. **Press F5** in VS Code to launch the extension in debug mode
2. In the new Extension Development Host window, open a folder that contains a `model.xml` file
3. Check the Debug Console in the original VS Code window for log output

## Test Project Structure

Create a test folder with this structure:
```
test-project/
  model.xml
  src/
    Main.java (optional)
```

## Debugging Tips

### Check the Debug Console
All Java backend output and extension logs appear in the Debug Console (View -> Debug Console)

Look for these log messages:
- `[JavaBridge] Initialized with jar path: ...`
- `[FOP] Loading model from: ...`
- `[JAVA stdout]` - Shows Java backend output
- `[JAVA stderr]` - Shows Java errors

### Common Issues

1. **JAR not found**: Build the Java backend first
   ```bash
   cd java-backend
   .\gradlew.bat build
   ```

2. **Model not found**: Ensure `model.xml` is in the root of your opened workspace folder

3. **JSON parse error**: Check the Debug Console for the raw Java output

### Manual Testing

Test the Java backend directly:
```powershell
java -jar "java-backend\build\libs\backend-1.0.0.jar" loadModel "path\to\model.xml"
```

## VS Code Commands

- **Ctrl+Shift+P** -> `FOP: Load Model` - Manually load a model
- **Ctrl+Shift+P** -> `FOP: Refresh Model` - Reload from workspace
- Click refresh icon in Feature Model tree view
