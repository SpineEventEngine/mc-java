# About `mc-java-signal-tests` module

This is a test-only module with the tests for the production code of `mc-java-signal`.

We need to have a separate test-only module because of the following reasons.

 * We want to run code generation tests using the vanilla Protobuf code placed into
resources by [ProtoTap][prototap] using [`PipelineSetup`][pipeline-setup] API.

 * We need this code to be "vanilla" Protobuf — not the one produced by the previous
version of McJava — because ProtoData plugins run on top of such code.

 * We use `testFixtures` source set for storing input proto files for the code generation tests.

 * It is not possible to apply a Gradle plugin to a source set. Plugins are applied to a project.

 * The module `mc-java-signal` needs McJava plugin for generating events and entity states for
the Signal ProtoData plugin. Once McJava plugin is applied to a project, it serves all 
the source sets, including `testFixtures`, making the generated code enhanced with Spine features,
which we need to avoid.

Therefore, to have "vanilla" Protobuf code generated for our stub types, we need to have
a separate Gradle project

[prototap]: https://github.com/SpineEventEngine/ProtoTap
[pipeline-setup]: https://github.com/SpineEventEngine/ProtoData/blob/master/testlib/src/main/kotlin/io/spine/protodata/testing/PipelineSetup.kt
