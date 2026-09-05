#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"
fixture_source="${script_dir}/configuration-cache-fixture"
local_repository="${repo_root}/build/localMaven"

agp_version="${AROUTER_AGP_VERSION:-7.4.2}"
wrapper_properties_name="${AROUTER_GRADLE_WRAPPER_PROPERTIES:-gradle-wrapper.properties}"
plugin_pipeline="${AROUTER_PLUGIN_PIPELINE:-scoped}"
use_configuration_cache="${AROUTER_CONFIGURATION_CACHE:-true}"
keep_test_root="${AROUTER_KEEP_TEST_ROOT:-false}"
compile_sdk="${AROUTER_COMPILE_SDK:-33}"
build_tools_version="${AROUTER_BUILD_TOOLS:-33.0.2}"
run_device_tests="${AROUTER_RUN_DEVICE_TESTS:-false}"

case "${run_device_tests}" in
    true|false) ;;
    *) echo "AROUTER_RUN_DEVICE_TESTS must be true or false." >&2; exit 1 ;;
esac
if [[ "${run_device_tests}" == true ]]; then
    adb="${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT for device tests}/platform-tools/adb"
    device_list="$("${adb}" devices)"
    serial="$(awk 'NR > 1 && $2 == "device" { print $1 }' <<< "${device_list}")"
    if [[ "$(awk 'NR > 1 && NF { count++ } END { print count+0 }' <<< "${device_list}")" != 1 \
            || "${serial}" != emulator-* ]]; then
        echo "Connect exactly one booted emulator for the consumer device tests." >&2
        exit 1
    fi
    if [[ -n "${ANDROID_SERIAL:-}" && "${ANDROID_SERIAL}" != "${serial}" ]]; then
        echo "ANDROID_SERIAL does not match the consumer test emulator." >&2
        exit 1
    fi
    export ANDROID_SERIAL="${serial}"
    if [[ "$("${adb}" shell getprop sys.boot_completed | tr -d '\r')" != 1 ]]; then
        echo "The consumer test emulator has not finished booting." >&2
        exit 1
    fi
fi

case "${plugin_pipeline}" in
    legacy|scoped) ;;
    *)
        echo "Unsupported ARouter plugin pipeline: ${plugin_pipeline}" >&2
        exit 1
        ;;
esac

case "${use_configuration_cache}:${keep_test_root}" in
    true:true|true:false|false:true|false:false) ;;
    *)
        echo "AROUTER_CONFIGURATION_CACHE and AROUTER_KEEP_TEST_ROOT must be true or false." >&2
        exit 1
        ;;
esac

if [[ "${wrapper_properties_name}" = /* ]]; then
    wrapper_properties="${wrapper_properties_name}"
else
    wrapper_properties="${fixture_source}/${wrapper_properties_name}"
fi
if [[ ! -f "${wrapper_properties}" ]]; then
    echo "Missing Gradle wrapper properties: ${wrapper_properties}" >&2
    exit 1
fi

read_version() {
    awk -F= '$1 == "VERSION_NAME" { print $2; exit }' "$1"
}

register_version="$(read_version "${repo_root}/arouter-gradle-plugin/gradle.properties")"
api_version="$(read_version "${repo_root}/arouter-api/gradle.properties")"
compiler_version="$(read_version "${repo_root}/arouter-compiler/gradle.properties")"

required_artifacts=(
    "${local_repository}/com/alibaba/arouter-register/${register_version}/arouter-register-${register_version}.jar"
    "${local_repository}/com/alibaba/arouter-api/${api_version}/arouter-api-${api_version}.aar"
    "${local_repository}/com/alibaba/arouter-compiler/${compiler_version}/arouter-compiler-${compiler_version}.jar"
)
for artifact in "${required_artifacts[@]}"; do
    if [[ ! -f "${artifact}" ]]; then
        echo "Missing local ARouter artifact: ${artifact}" >&2
        echo "Run the four module installLocally tasks before this verifier." >&2
        exit 1
    fi
done

test_root="$(mktemp -d "${TMPDIR:-/tmp}/arouter-gradle-plugin.XXXXXX")"
cleanup() {
    if [[ -d "${test_root}" ]]; then
        if [[ "${keep_test_root}" == true ]]; then
            echo "Preserved ARouter Gradle plugin fixture at ${test_root}."
            return
        fi
        ls -la "${test_root}" >/dev/null
        rm -rf -- "${test_root}"
    fi
}
trap cleanup EXIT

project_dir="${test_root}/project"
wrapper_dir="${test_root}/wrapper/gradle/wrapper"
mkdir -p "${project_dir}" "${wrapper_dir}"
rsync -a --exclude '.gradle/' --exclude 'build/' "${fixture_source}/" "${project_dir}/"
cp "${repo_root}/gradle/wrapper/gradle-wrapper.jar" "${wrapper_dir}/gradle-wrapper.jar"
cp "${wrapper_properties}" "${wrapper_dir}/gradle-wrapper.properties"

gradle_command=(
    java
    -Dorg.gradle.appname=gradlew
    -classpath "${wrapper_dir}/gradle-wrapper.jar"
    org.gradle.wrapper.GradleWrapperMain
    -p "${project_dir}"
    --no-daemon
    --console=plain
)
if [[ "${use_configuration_cache}" == true ]]; then
    gradle_command+=(
        --configuration-cache
        --configuration-cache-problems=fail
    )
fi
gradle_command+=(
    "-Parouter.repository=${local_repository}"
    "-Parouter.register.version=${register_version}"
    "-Parouter.api.version=${api_version}"
    "-Parouter.compiler.version=${compiler_version}"
    "-Parouter.agp.version=${agp_version}"
    "-Parouter.compile.sdk=${compile_sdk}"
    "-Parouter.build.tools=${build_tools_version}"
    "-Parouter.min.sdk=${AROUTER_MIN_SDK:-14}"
    "-Parouter.androidx.fragment.version=${AROUTER_ANDROIDX_FRAGMENT_VERSION:-1.0.0}"
)
if [[ -n "${AROUTER_ANDROIDX_CORE_VERSION:-}" ]]; then
    gradle_command+=("-Parouter.androidx.core.version=${AROUTER_ANDROIDX_CORE_VERSION}")
fi
if [[ -n "${AROUTER_GRADLE_INIT_SCRIPT:-}" ]]; then
    gradle_command+=(--init-script "${AROUTER_GRADLE_INIT_SCRIPT}")
fi
assemble_tasks=(
    :app:assembleDailyDebug
    :app:assembleDailyRelease
    :app:assembleOnlineDebug
    :app:assembleOnlineRelease
)

find_transformed_jar() {
    local variant="$1"
    local flavor="$2"
    local build_type="$3"
    local search_root

    if [[ "${plugin_pipeline}" == scoped ]]; then
        search_root="${project_dir}/app/build/intermediates/classes/${variant}"
    else
        search_root="${project_dir}/app/build/intermediates/transforms/com.alibaba.arouter/${flavor}/${build_type}"
    fi

    if [[ ! -d "${search_root}" ]]; then
        return 1
    fi

    while IFS= read -r candidate; do
        if jar tf "${candidate}" | grep -Fxq 'com/alibaba/android/arouter/core/LogisticsCenter.class'; then
            echo "${candidate}"
            return 0
        fi
    done < <(find "${search_root}" -type f -name '*.jar' | sort)
    return 1
}

verify_registrations() {
    local variant="$1"
    local flavor="$2"
    local build_type="$3"
    local feature_module="$4"
    local transformed_jar
    local actual
    local expected

    if ! transformed_jar="$(find_transformed_jar "${variant}" "${flavor}" "${build_type}")"; then
        echo "Cannot find transformed LogisticsCenter for ${variant}." >&2
        exit 1
    fi

    actual="$(
        javap -classpath "${transformed_jar}" -c -p \
            com.alibaba.android.arouter.core.LogisticsCenter |
            awk '
                /^  private static void loadRouterMap\(\);$/ { capture = 1; next }
                capture && /^  (public|protected|private) / { exit }
                capture { print }
            ' |
            grep -F '// String com.alibaba.android.arouter.routes.ARouter$$' |
            sed 's/^.*\/\/ String //'
    )"
    expected="$(
        printf '%s\n' \
            'com.alibaba.android.arouter.routes.ARouter$$Providers$$app' \
            'com.alibaba.android.arouter.routes.ARouter$$Providers$$arouterapi' \
            "com.alibaba.android.arouter.routes.ARouter\$\$Providers\$\$${feature_module}" \
            'com.alibaba.android.arouter.routes.ARouter$$Root$$app' \
            'com.alibaba.android.arouter.routes.ARouter$$Root$$arouterapi' \
            "com.alibaba.android.arouter.routes.ARouter\$\$Root\$\$${feature_module}"
    )"

    if [[ "${actual}" != "${expected}" ]]; then
        echo "Unexpected route registrations for ${variant}." >&2
        echo "Expected:" >&2
        echo "${expected}" >&2
        echo "Actual:" >&2
        echo "${actual}" >&2
        exit 1
    fi
}

verify_all_registrations() {
    verify_registrations dailyDebug daily debug featuredaily
    verify_registrations dailyRelease daily release featuredaily
    verify_registrations onlineDebug online debug featureonline
    verify_registrations onlineRelease online release featureonline
}

verify_apk_routes() {
    local apk="$1"
    local feature_module="$2"
    local excluded_feature_module="$3"
    local dex_dir="${test_root}/dex-${feature_module}-$(basename "${apk}" .apk)"
    local descriptors
    local expected_descriptor
    local excluded_descriptor
    local dexdump="${ANDROID_SDK_ROOT}/build-tools/${build_tools_version}/dexdump"

    if [[ ! -x "${dexdump}" ]]; then
        echo "Missing dexdump executable: ${dexdump}" >&2
        exit 1
    fi

    mkdir -p "${dex_dir}"
    unzip -q "${apk}" 'classes*.dex' -d "${dex_dir}"
    descriptors="$(
        for dex_file in "${dex_dir}"/classes*.dex; do
            "${dexdump}" "${dex_file}" | grep -F 'Class descriptor  :'
        done
    )"

    for route_contract in IProviderGroup IRouteRoot; do
        expected_descriptor="Lcom/alibaba/android/arouter/facade/template/${route_contract};"
        if ! grep -Fq "${expected_descriptor}" <<< "${descriptors}"; then
            echo "Missing ${route_contract} from ${apk}." >&2
            exit 1
        fi
    done

    for route_kind in Providers Root; do
        expected_descriptor="Lcom/alibaba/android/arouter/routes/ARouter\$\$${route_kind}\$\$${feature_module};"
        excluded_descriptor="Lcom/alibaba/android/arouter/routes/ARouter\$\$${route_kind}\$\$${excluded_feature_module};"
        grep -Fq "${expected_descriptor}" <<< "${descriptors}"
        if grep -Fq "${excluded_descriptor}" <<< "${descriptors}"; then
            echo "Unexpected ${excluded_feature_module} route class in ${apk}." >&2
            exit 1
        fi
    done
}

first_log="${test_root}/first-build.log"
second_log="${test_root}/second-build.log"
incremental_log="${test_root}/incremental-build.log"

"${gradle_command[@]}" "${assemble_tasks[@]}" | tee "${first_log}"
if [[ "${use_configuration_cache}" == true ]]; then
    grep -F "Configuration cache entry stored." "${first_log}"

    "${gradle_command[@]}" "${assemble_tasks[@]}" | tee "${second_log}"
    grep -F "Reusing configuration cache." "${second_log}"
    grep -F "Configuration cache entry reused." "${second_log}"
fi
verify_all_registrations

perl -pi -e 's#/cache/second#/cache/updated#g' \
    "${project_dir}/app/src/main/java/com/alibaba/android/arouter/configcache/ProbeActivity.java" \
    "${project_dir}/app/src/main/java/com/alibaba/android/arouter/configcache/SecondActivity.java"

"${gradle_command[@]}" "${assemble_tasks[@]}" | tee "${incremental_log}"
if [[ "${use_configuration_cache}" == true ]]; then
    grep -F "Reusing configuration cache." "${incremental_log}"
    grep -F "Configuration cache entry reused." "${incremental_log}"
fi

for variant in DailyDebug DailyRelease OnlineDebug OnlineRelease; do
    if [[ "${plugin_pipeline}" == scoped ]]; then
        transform_task="transform${variant}ClassesWithARouter"
    else
        transform_task="transformClassesWithCom.alibaba.arouterFor${variant}"
    fi
    grep -F "> Task :app:${transform_task}" "${incremental_log}"
done
verify_all_registrations

generated_groups=0
while IFS= read -r -d '' generated_group; do
    grep -F '"/cache/updated"' "${generated_group}"
    generated_groups=$((generated_groups + 1))
done < <(find "${project_dir}/app/build/generated" -type f \
    -name "ARouter\$\$Group\$\$cache.java" -print0)

if [[ ${generated_groups} -ne 4 ]]; then
    echo "Expected four generated cache route groups, found ${generated_groups}." >&2
    exit 1
fi

daily_debug_apk="${project_dir}/app/build/outputs/apk/daily/debug/app-daily-debug.apk"
daily_release_apk="${project_dir}/app/build/outputs/apk/daily/release/app-daily-release.apk"
online_debug_apk="${project_dir}/app/build/outputs/apk/online/debug/app-online-debug.apk"
online_release_apk="${project_dir}/app/build/outputs/apk/online/release/app-online-release.apk"

for apk in \
    "${daily_debug_apk}" \
    "${daily_release_apk}" \
    "${online_debug_apk}" \
    "${online_release_apk}"; do
    test -s "${apk}"
done

verify_apk_routes "${daily_debug_apk}" featuredaily featureonline
verify_apk_routes "${daily_release_apk}" featuredaily featureonline
verify_apk_routes "${online_debug_apk}" featureonline featuredaily
verify_apk_routes "${online_release_apk}" featureonline featuredaily

if [[ "${run_device_tests}" == true ]]; then
    # Assembly/configuration-cache contracts were checked above. Device tasks
    # are run separately, against both flavors and the genuinely shrunk APKs.
    for test_type in debug release; do
        if [[ "${test_type}" == debug ]]; then test_variant=Debug; else test_variant=Release; fi
        report_marker="$(mktemp "${test_root}/device-${test_type}.XXXXXX")"
        "${gradle_command[@]}" --no-configuration-cache \
            "-Parouter.test.build.type=${test_type}" \
            ":app:connectedDaily${test_variant}AndroidTest" \
            ":app:connectedOnline${test_variant}AndroidTest" \
            | tee "${test_root}/device-${test_type}.log"
        report_count=0
        while IFS= read -r -d '' report; do
            if ! grep -Eq '<testsuite .*tests="[1-9][0-9]*"' "${report}" \
                    || grep -Eq ' (failures|errors|skipped)="[1-9][0-9]*"' "${report}"; then
                echo "Empty, failed, or skipped consumer device tests in ${report}." >&2
                exit 1
            fi
            report_count=$((report_count + 1))
        done < <(find "${project_dir}/app/build/outputs/androidTest-results/connected" \
            -type f -name 'TEST-*.xml' -newer "${report_marker}" -print0)
        if [[ ${report_count} -ne 2 ]]; then
            echo "Expected fresh ${test_type} reports for both consumer flavors, found ${report_count}." >&2
            exit 1
        fi
    done
fi

echo "ARouter Gradle plugin verification passed for AGP ${agp_version} (${plugin_pipeline})."
