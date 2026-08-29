## Summary / 变更说明

Describe the problem and the smallest change that solves it.
请说明问题，以及解决该问题所需的最小改动。

## Related issue / 关联 Issue

Closes #

## Compatibility / 兼容性

- Android/AGP/Gradle/JDK/Kotlin versions affected:
- Public API or binary compatibility impact:
- Runtime registration mode affected:

## Validation / 验证

- [ ] `./gradlew :arouter-gradle-plugin:build` followed by `./gradlew -Parouter.useLocalRegisterPlugin :arouter-annotation:build :arouter-compiler:build :arouter-api:assemble assembleDebug`
- [ ] Added or updated a focused regression test, or explained why no test is possible.
- [ ] Verified the change does not include generated files, build output, credentials, or unrelated formatting.
- [ ] The pull request targets `master`.
- [ ] All commit authors have completed the required CLA check.
