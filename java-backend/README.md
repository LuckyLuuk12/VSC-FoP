# Setup

FSTMerge uses "merge" which is included in the rcs toolchain

```
sudo apt install rcs
```

# Run

```
./gradlew run --args="buildVariant <configFile>"
```

# Example

```
./gradlew run --args="buildVariant TESTPROJ_Stack/Stack.features"
```

The resulting merged program is located at: 
```
TESTPROJ_Stack/Stack/Stack.java.merge
TESTPROJ_Stack/Stack/Test.java.merge
```
