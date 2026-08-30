#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"
fixture_source="${script_dir}/configuration-cache-fixture"
local_repository="${repo_root}/build/localMaven"

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

test_root="$(mktemp -d "${TMPDIR:-/tmp}/arouter-configuration-cache.XXXXXX")"
cleanup() {
    if [[ -d "${test_root}" ]]; then
        ls -la "${test_root}" >/dev/null
        rm -rf -- "${test_root}"
    fi
}
trap cleanup EXIT

project_dir="${test_root}/project"
wrapper_dir="${test_root}/wrapper/gradle/wrapper"
cp -R "${fixture_source}" "${project_dir}"
mkdir -p "${wrapper_dir}"
cp "${repo_root}/gradle/wrapper/gradle-wrapper.jar" "${wrapper_dir}/gradle-wrapper.jar"
cp "${fixture_source}/gradle-wrapper.properties" "${wrapper_dir}/gradle-wrapper.properties"

gradle_command=(
    java
    -Dorg.gradle.appname=gradlew
    -classpath "${wrapper_dir}/gradle-wrapper.jar"
    org.gradle.wrapper.GradleWrapperMain
    -p "${project_dir}"
    --no-daemon
    --console=plain
    --configuration-cache
    --configuration-cache-problems=fail
    "-Parouter.repository=${local_repository}"
    "-Parouter.register.version=${register_version}"
    "-Parouter.api.version=${api_version}"
    "-Parouter.compiler.version=${compiler_version}"
    :app:assembleDailyDebug
    :app:assembleDailyRelease
    :app:assembleOnlineDebug
    :app:assembleOnlineRelease
)

first_log="${test_root}/first-build.log"
second_log="${test_root}/second-build.log"
incremental_log="${test_root}/incremental-build.log"

"${gradle_command[@]}" | tee "${first_log}"
grep -F "0 problems were found storing the configuration cache." "${first_log}"
grep -F "Configuration cache entry stored." "${first_log}"

"${gradle_command[@]}" | tee "${second_log}"
grep -F "Reusing configuration cache." "${second_log}"
grep -F "Configuration cache entry reused." "${second_log}"

perl -pi -e 's#/cache/second#/cache/updated#g' \
    "${project_dir}/app/src/main/java/com/alibaba/android/arouter/configcache/ProbeActivity.java" \
    "${project_dir}/app/src/main/java/com/alibaba/android/arouter/configcache/SecondActivity.java"

"${gradle_command[@]}" | tee "${incremental_log}"
grep -F "Reusing configuration cache." "${incremental_log}"
grep -F "Configuration cache entry reused." "${incremental_log}"

for variant in DailyDebug DailyRelease OnlineDebug OnlineRelease; do
    grep -Fx "> Task :app:transform${variant}ClassesWithARouter" "${incremental_log}"
done

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

for apk in \
    "${project_dir}/app/build/outputs/apk/daily/debug/app-daily-debug.apk" \
    "${project_dir}/app/build/outputs/apk/daily/release/app-daily-release.apk" \
    "${project_dir}/app/build/outputs/apk/online/debug/app-online-debug.apk" \
    "${project_dir}/app/build/outputs/apk/online/release/app-online-release.apk"; do
    test -s "${apk}"
done

echo "ARouter configuration-cache verification passed."
