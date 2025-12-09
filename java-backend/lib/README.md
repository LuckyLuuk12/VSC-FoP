This folder contains all different tools VSC-FoP supports

Download the FeatureHouse.jar to the lib folder
```powershell
Invoke-WebRequest -Uri "https://www.se.cs.uni-saarland.de/apel/fh/deploy/FeatureHouse-2011-03-15.jar" -OutFile "java-backend\lib\FeatureHouse.jar" 
```

VSCode and other IDEs might show errors when using FeatureHouse like this:
```java
composer.FSTGenComposer.main({})
```

But just build it with gradlew and it should work fine.


