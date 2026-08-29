# Contributing to ARouter

Thank you for helping maintain ARouter. Submit changes to the `develop` integration branch. Maintainers merge validated release batches from `develop` into the release-only `master` branch.

感谢你参与 ARouter 维护。所有变更都应提交到集成分支 `develop`；维护者会在发布批次验证通过后，将 `develop` 合并到仅用于发布的 `master`。

## Before opening an issue

Search open and closed issues first. Reproducible defects should use the bug form; integration questions and general support belong in [GitHub Discussions](https://github.com/alibaba/ARouter/discussions).

请先搜索已有 Issue。可复现缺陷使用 Bug 模板；接入咨询和一般使用问题请发到 GitHub Discussions。

## Local build

The current maintenance baseline intentionally preserves the legacy toolchain while modernization work is planned separately:

- JDK 8
- Gradle 6.5 via the checked-in wrapper
- Android SDK Platform 29
- Android Build Tools 29.0.2

Run the same core verification used by CI:

```shell
./gradlew --no-daemon --stacktrace :arouter-gradle-plugin:build
./gradlew --no-daemon --stacktrace -Parouter.useLocalRegisterPlugin \
  :arouter-annotation:build \
  :arouter-compiler:build \
  :arouter-api:assemble \
  assembleDebug
```

## Pull requests

- Target `develop` and link a focused issue.
- Keep compatibility fixes separate from large modernization work.
- Add a minimal regression test or reproducible verification whenever possible.
- Do not commit generated sources, build output, local SDK settings, credentials, or unrelated formatting.
- Describe API, binary, Android, AGP, Gradle, JDK, Kotlin, and minSdk compatibility impact.
- Complete the repository CLA check before merge.

Large changes such as AndroidX, modern AGP instrumentation, KSP, Compose, or dynamic-feature support should start with a design issue so compatibility and migration policy can be agreed before implementation.
