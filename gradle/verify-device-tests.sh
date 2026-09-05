#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"
build_type="${1:-debug}"
if [[ $# -gt 0 ]]; then shift; fi
case "${build_type}" in
    debug) variant=Debug ;;
    release) variant=Release ;;
    *) echo "Usage: $0 [debug|release] [Gradle options...]" >&2; exit 1 ;;
esac

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
adb="${sdk_root}/platform-tools/adb"
if [[ ! -x "${adb}" ]]; then adb="$(command -v adb)"; fi
device_list="$("${adb}" devices)"
devices=()
while IFS= read -r serial; do
    [[ -z "${serial}" ]] || devices+=("${serial}")
done < <(awk 'NR > 1 && NF { print $1 }' <<< "${device_list}")

# AGP's connected tasks must not accidentally include another device. This
# verifier deliberately requires one booted emulator, including on local runs.
if [[ ${#devices[@]} -ne 1 || "${devices[0]}" != emulator-* ]]; then
    echo "Connect exactly one booted emulator before running device tests." >&2
    exit 1
fi
serial="${devices[0]}"
if [[ -n "${ANDROID_SERIAL:-}" && "${ANDROID_SERIAL}" != "${serial}" ]]; then
    echo "ANDROID_SERIAL does not match the connected emulator." >&2
    exit 1
fi
export ANDROID_SERIAL="${serial}"
if [[ "$("${adb}" -s "${serial}" shell getprop sys.boot_completed | tr -d '\r')" != 1 ]]; then
    echo "The emulator has not finished booting." >&2
    exit 1
fi
api_level="$("${adb}" -s "${serial}" shell getprop ro.build.version.sdk | tr -d '\r')"
if [[ -n "${AROUTER_EXPECT_API:-}" && "${api_level}" != "${AROUTER_EXPECT_API}" ]]; then
    echo "Expected API ${AROUTER_EXPECT_API}, connected API ${api_level}." >&2
    exit 1
fi

cd "${repo_root}"
gradle_command=(./gradlew --no-daemon --console=plain --stacktrace "$@")
"${gradle_command[@]}" :arouter-gradle-plugin:build
tasks=(":app:connected${variant}AndroidTest")
report_modules=(app)
if [[ "${build_type}" == debug ]]; then
    tasks=(:arouter-api:connectedDebugAndroidTest "${tasks[@]}")
    report_modules+=(arouter-api)
fi

echo "Running all ${build_type} device tests on ${serial} (API ${api_level})."
run_marker="$(mktemp "${TMPDIR:-/tmp}/arouter-device-run.XXXXXX")"
trap 'rm -f -- "${run_marker}"' EXIT
"${gradle_command[@]}" -Parouter.useLocalRegisterPlugin \
    "-Parouter.testBuildType=${build_type}" "${tasks[@]}"

for module in "${report_modules[@]}"; do
    report_dir="${repo_root}/${module}/build/outputs/androidTest-results/connected"
    archive_dir="${repo_root}/build/reports/device-tests/api-${api_level}/${build_type}/${module}"
    mkdir -p "${archive_dir}"
    report_count=0
    while IFS= read -r -d '' report; do
        if ! grep -Eq '<testsuite .*tests="[1-9][0-9]*"' "${report}" \
                || grep -Eq ' (failures|errors|skipped)="[1-9][0-9]*"' "${report}"; then
            echo "Empty, failed, or skipped device tests in ${report}." >&2
            exit 1
        fi
        cp "${report}" "${archive_dir}/$(basename "${report}")"
        report_count=$((report_count + 1))
    done < <(find "${report_dir}" -type f -name 'TEST-*.xml' -newer "${run_marker}" -print0)
    if [[ ${report_count} -eq 0 ]]; then
        echo "Missing device-test reports for ${module}." >&2
        exit 1
    fi
done
if [[ "${build_type}" == release ]]; then
    test -s app/build/outputs/mapping/release/mapping.txt
fi
echo "ARouter ${build_type} device tests passed on API ${api_level}."
