#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"
plugin_dir="${repo_root}/arouter-idea-plugin"

test_root="$(mktemp -d "${TMPDIR:-/tmp}/arouter-idea-plugin.XXXXXX")"
cleanup() {
    if [[ -d "${test_root}" ]]; then
        ls -la "${test_root}" >/dev/null
        rm -rf -- "${test_root}"
    fi
}
trap cleanup EXIT

wrapper_dir="${test_root}/wrapper/gradle/wrapper"
mkdir -p "${wrapper_dir}"
cp "${repo_root}/gradle/wrapper/gradle-wrapper.jar" "${wrapper_dir}/gradle-wrapper.jar"
cp "${plugin_dir}/gradle/wrapper/gradle-wrapper.properties" \
    "${wrapper_dir}/gradle-wrapper.properties"

java_command="java"
if [[ -n "${AROUTER_JAVA_HOME:-}" ]]; then
    java_command="${AROUTER_JAVA_HOME}/bin/java"
elif [[ -x /usr/libexec/java_home ]]; then
    detected_jdk21="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
    if [[ -x "${detected_jdk21}/bin/java" ]]; then
        java_command="${detected_jdk21}/bin/java"
    fi
fi

gradle_command=(
    "${java_command}"
    -Dorg.gradle.appname=gradlew
    -classpath "${wrapper_dir}/gradle-wrapper.jar"
    org.gradle.wrapper.GradleWrapperMain
    -p "${plugin_dir}"
    --no-daemon
    --console=plain
    --stacktrace
    --rerun-tasks
    --configuration-cache
    --configuration-cache-problems=fail
)

verification_tasks=(
    cleanSandbox
    test
    buildPlugin
    verifyPluginProjectConfiguration
    verifyPluginStructure
    verifyPlugin
)

"${gradle_command[@]}" "${verification_tasks[@]}"

cache_output="$("${gradle_command[@]}" "${verification_tasks[@]}" 2>&1)"
if ! grep -Fq 'Configuration cache entry reused' <<<"${cache_output}"; then
    # A cold IntelliJ Platform resolution can materialize a bundled JBR after Gradle has
    # snapshotted JavaRuntimeMetadataValueSource. Allow that cache to stabilize once, then
    # require the identical task graph to reuse it.
    cache_output="$("${gradle_command[@]}" "${verification_tasks[@]}" 2>&1)"
fi
if ! grep -Fq 'Configuration cache entry reused' <<<"${cache_output}"; then
    echo "IDE plugin build did not reuse the Gradle configuration cache." >&2
    echo "${cache_output}" >&2
    exit 1
fi

test_result="${plugin_dir}/build/test-results/test/TEST-com.alibaba.android.arouter.idea.extensions.NavigationLineMarkerTest.xml"
if [[ ! -f "${test_result}" ]] || \
        ! grep -Eq 'tests="6"[^>]*failures="0"' "${test_result}"; then
    echo "IDE plugin functional test report is missing or contains failures." >&2
    exit 1
fi

plugin_version="$(awk -F= '$1 == "arouterIdeaPluginVersion" { print $2; exit }' \
    "${plugin_dir}/gradle.properties")"
plugin_archive="${plugin_dir}/build/distributions/arouter-idea-plugin-${plugin_version}.zip"
if [[ ! -f "${plugin_archive}" ]]; then
    echo "IDE plugin archive is missing: ${plugin_archive}" >&2
    exit 1
fi
if unzip -Z1 "${plugin_archive}" | \
        grep -Eq 'kotlin-stdlib|junit|hamcrest|arouter-test-api'; then
    echo "IDE plugin archive contains a test-only or platform-provided dependency." >&2
    exit 1
fi

plugin_jar="${test_root}/arouter-idea-plugin.jar"
unzip -p "${plugin_archive}" \
    "arouter-idea-plugin/lib/arouter-idea-plugin-${plugin_version}.jar" >"${plugin_jar}"
descriptor="$(unzip -p "${plugin_jar}" META-INF/plugin.xml)"
grep -Fq '<idea-version since-build="243"' <<<"${descriptor}"
grep -Fq 'language="UAST"' <<<"${descriptor}"
grep -Fq '<supportsKotlinPluginMode supportsK2="true"' <<<"${descriptor}"
if grep -Fq 'until-build=' <<<"${descriptor}"; then
    echo "IDE plugin descriptor unexpectedly restricts future IDE builds." >&2
    exit 1
fi

for product_code in IC AI; do
    verdict="$(find "${plugin_dir}/build/reports/pluginVerifier" -type f \
        -path "*/${product_code}-*/plugins/arouter-roadsign/*/verification-verdict.txt" \
        -print -quit)"
    if [[ -z "${verdict}" ]] || ! grep -Fxq 'Compatible' "${verdict}"; then
        echo "IntelliJ Plugin Verifier did not report ${product_code} as compatible." >&2
        exit 1
    fi
done

echo "ARouter IDE plugin verification passed."
