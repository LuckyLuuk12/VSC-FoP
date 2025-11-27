# Run

```
./gradlew run --args="buildVariant <configFile> <featureFolder> <outputPath>"
```

# Example Run

Output path does not work yet, so example usage would be:

```
./gradlew run --args="buildVariant ../test-configs/auth-cli.xml ../test-features ."
```

The resulting program will be put in "test-features/tmp"
