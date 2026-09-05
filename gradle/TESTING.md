# AndroidX development checks

These checks validate development artifacts, not a release or publication.
ARouter's published minimum dependencies and `minSdkVersion=14` are unchanged.
Device coverage starts at API 21; it does not establish runtime coverage for
every API level between 14 and 20.

## Complete framework device suite

Use JDK 8 and connect exactly one booted Android emulator. The script rejects
physical devices, extra devices, and a mismatched `ANDROID_SERIAL`.

```sh
export ANDROID_HOME=/path/to/android-sdk
export JAVA_HOME=/path/to/jdk8
AROUTER_EXPECT_API=21 ./gradle/verify-device-tests.sh debug
AROUTER_EXPECT_API=21 ./gradle/verify-device-tests.sh release
```

Repeat on API 34. Additional arguments are passed to Gradle, for example
`--init-script /path/to/local-repository-settings.gradle` for a local mirror.
CI uses the configured upstream repositories, without a mirror override.

Debug runs all API-module and demo instrumentation tests. Release runs the demo
suite against the minified target APK. These cover Java/Kotlin routing, provider
and Fragment routes, parameter injection, interceptors and redirection, task
flags, result delivery, missing routes, and R8 behavior. The demo's narrow
test-only keep rules preserve APIs called directly from the separate test APK;
they are not part of the published consumer rules.

The verifier requires fresh, nonempty JUnit reports with no failures, errors,
or skipped tests. Local copies are retained by API and build type under
`build/reports/device-tests/`, so later runs do not erase earlier matrix results.

## Published-artifact consumer

First stage all four local artifacts using JDK 8:

```sh
./gradlew :arouter-annotation:installLocally :arouter-compiler:installLocally \
  :arouter-api:installLocally :arouter-gradle-plugin:installLocally
./gradle/verify-androidx.sh
```

The existing Gradle plugin matrix checks the minimum AndroidX dependency set.
An additional API 34 consumer job uses Core 1.17.0 and Fragment 1.8.9, pinned
stable versions compatible with the fixture's SDK 36 toolchain. This is a
consumer dependency override, not an upgrade of ARouter's published minimums.
See the [Core](https://developer.android.com/jetpack/androidx/releases/core#1.17.0)
and [Fragment](https://developer.android.com/jetpack/androidx/releases/fragment#1.8.9)
release notes for these baselines.

With JDK 17 on `PATH`, `ANDROID_SDK_ROOT` set, and an API 34 emulator booted:

```sh
AROUTER_AGP_VERSION=9.3.2 \
AROUTER_GRADLE_WRAPPER_PROPERTIES=gradle-wrapper-9.5.properties \
AROUTER_COMPILE_SDK=36 AROUTER_BUILD_TOOLS=36.0.0 \
AROUTER_MIN_SDK=23 \
AROUTER_ANDROIDX_CORE_VERSION=1.17.0 \
AROUTER_ANDROIDX_FRAGMENT_VERSION=1.8.9 \
AROUTER_RUN_DEVICE_TESTS=true AROUTER_KEEP_TEST_ROOT=true \
./gradle/verify-configuration-cache.sh
```

This verifies configuration-cache reuse, incremental route updates, flavor
isolation, and all four daily/online Debug/Release variants. Device tests launch
the consumer through Android components and check actual routing after provider,
Fragment argument/injection, and ActivityOptionsCompat checks. There are no
test-only keep rules in this consumer, including its Release/R8 runs. Jetifier
is disabled throughout.

CI preserves device reports and R8 mappings even when tests fail. Set
`AROUTER_KEEP_TEST_ROOT=true` to retain local fixture output; its exact path is
printed by the verifier. `AROUTER_GRADLE_INIT_SCRIPT` optionally supplies a local
repository override to the fixture.
