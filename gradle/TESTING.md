# AndroidX development checks

These checks validate development artifacts, not a release or publication.
ARouter's published minimum dependencies and `minSdkVersion=14` are unchanged.
The complete demo CI matrix runs on API 21 and API 34. Minimum-API framework
and consumer checks are separate; they do not establish coverage for every
intermediate API level.

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

## Minimum API 14 runtime checks

Connect exactly one booted API 14 emulator and verify its API level before
running the standalone framework tests with JDK 8:

```sh
"${ANDROID_HOME}/platform-tools/adb" shell getprop ro.build.version.sdk
./gradlew :arouter-api:connectedDebugAndroidTest
```

For end-to-end routing and Release/R8, run the published-artifact consumer
below on the same emulator with the minimum dependencies and `AROUTER_MIN_SDK=14`.
This exercises both daily/online Debug/Release variants without test-only keep
rules. Preserve the fixture and check its fresh JUnit reports, not just APK
assembly. The consumer covers Activity and provider routing, Fragment and
DialogFragment creation, arguments and injection. `withOptionsCompat` requires
API 16 and is deliberately not exercised on API 14.

The demo's Gson 2.14 dependency requires [API 21 or newer](https://github.com/google/gson#minimum-android-api-level).
The demo application and its Gson-based `module-java` therefore use
`DEMO_MIN_SDK_VERSION=21`, separately from the runtime's `MIN_SDK_VERSION=14`.
Do not use the demo as proof of the runtime library's minimum API. Gson is a
JVM compiler/demo dependency, not an ARouter runtime dependency.

The official API 14 ARM image uses the old `kernel-qemu`/classic engine.
Emulator 36.1.9 cannot boot it. An isolated SDK Tools 25.2.5 classic engine
was validated on macOS through Rosetta; the [official macOS archive](https://dl.google.com/android/repository/tools_r25.2.5-macosx.zip)
can be extracted separately without replacing a working SDK emulator.
See the [SDK Tools release history](https://developer.android.com/tools/releases/sdk-tools)
and keep the chosen engine version and system-image checksum with test results.

## Published-artifact consumer

The dependency verifier requires `rg` (ripgrep), `unzip`, and `strings`
(`binutils` on Linux). CI installs these tools explicitly; they must also be on
`PATH` when running the verifier locally.

First stage all four local artifacts using JDK 8:

```sh
./gradlew :arouter-annotation:installLocally :arouter-compiler:installLocally \
  :arouter-api:installLocally :arouter-gradle-plugin:installLocally
./gradle/verify-androidx.sh
```

The Gradle plugin matrix checks the minimum AndroidX dependency set. Consumer
device jobs cover both the published minimum (Core 1.0.2 / Fragment 1.0.0 on
API 21) and upgraded dependencies (Core 1.17.0 / Fragment 1.8.9 on API 34), with
all four daily/online Debug/Release variants in each job. The upgraded versions
are pinned baselines compatible with the fixture's SDK 36 toolchain. This is a
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
Fragment/DialogFragment argument/injection, and ActivityOptionsCompat checks.
There are no test-only keep rules in this consumer, including its Release/R8 runs. Jetifier
is disabled throughout.

To run the minimum dependency job on an API 21 emulator, use the same command
with `AROUTER_MIN_SDK=14`, `AROUTER_ANDROIDX_CORE_VERSION=1.0.2`, and
`AROUTER_ANDROIDX_FRAGMENT_VERSION=1.0.0`. Do not substitute newer dependencies
for this check: newer Fragment artifacts have constructor keep rules that can
hide missing rules in ARouter. Fragment routes require public no-argument
constructors, and ARouter's own consumer rules preserve them. Routing a
DialogFragment only creates the instance; the caller owns displaying it.

CI preserves device reports and R8 mappings even when tests fail. Set
`AROUTER_KEEP_TEST_ROOT=true` to retain local fixture output; its exact path is
printed by the verifier. `AROUTER_GRADLE_INIT_SCRIPT` optionally supplies a local
repository override to the fixture.
