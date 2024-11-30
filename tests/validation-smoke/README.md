# Smoke test of Validation codegen

The purpose of this project is to generate the code using applied McJava plugin from
proto types that involve Validation codegen.

For example, this module declares command messages types that implicitly require having
the first field populated.

If Java compilation of the generated code succeeds, we assume that the smoke test
of the Validation codegen passed.
