# Integration tests

This module contains the integration tests for the Spine tools.

This module is a separate Gradle project and depends on `mc-java` and `mc-java-checks` via Gradle
[composite build][composite-build].

Tests in this module are run in a separate Gradle process and currently cannot be included in the 
repository code coverage.

They also should be run separately from the `base` project build like this:

```bash
  ./gradlew build check --stacktrace
  
  cd ./tests
  
  ./gradlew check --stacktrace
```

To configure IntelliJ IDEA for recognizing `tests` module as the actual source code, please see 
[this page][idea-composite-builds].
                                                                                                                                  
[composite-build]: https://docs.gradle.org/current/userguide/composite_builds.html#included_build_declaring_substitutions
[idea-composite-builds]: https://blog.jetbrains.com/idea/2016/10/intellij-idea-2016-3-eap-gradle-composite-builds-and-android-studio-2-2/ 
