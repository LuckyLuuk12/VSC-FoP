# Run

```
./gradlew run --args="buildVariant <configFile>"
```

# Example

Change "Graph/GraphComp.features" to include/exclude features

```
./gradlew run --args="buildVariant test/Graph/GraphComp.features"
```

The resulting program will be put in "test/Graph/GraphComp/Graph/"
