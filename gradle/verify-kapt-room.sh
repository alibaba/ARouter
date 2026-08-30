#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"
fixture_source="${script_dir}/kapt-room-fixture"
local_repository="${repo_root}/build/localMaven"

read_version() {
    awk -F= '$1 == "VERSION_NAME" { print $2; exit }' "$1"
}

annotation_version="$(read_version "${repo_root}/arouter-annotation/gradle.properties")"
compiler_version="$(read_version "${repo_root}/arouter-compiler/gradle.properties")"
annotation_artifact="${local_repository}/com/alibaba/arouter-annotation/${annotation_version}/arouter-annotation-${annotation_version}.jar"
compiler_artifact="${local_repository}/com/alibaba/arouter-compiler/${compiler_version}/arouter-compiler-${compiler_version}.jar"

for artifact in "${annotation_artifact}" "${compiler_artifact}"; do
    if [[ ! -f "${artifact}" ]]; then
        echo "Missing local ARouter artifact: ${artifact}" >&2
        echo "Run :arouter-annotation:installLocally and :arouter-compiler:installLocally first." >&2
        exit 1
    fi
done

test_root="$(mktemp -d "${TMPDIR:-/tmp}/arouter-kapt-room.XXXXXX")"
cleanup() {
    if [[ -d "${test_root}" ]]; then
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
cp "${fixture_source}/gradle-wrapper.properties" "${wrapper_dir}/gradle-wrapper.properties"

java_command="java"
if [[ -n "${AROUTER_JAVA_HOME:-}" ]]; then
    java_command="${AROUTER_JAVA_HOME}/bin/java"
elif [[ -x /usr/libexec/java_home ]]; then
    detected_jdk11="$(/usr/libexec/java_home -v 11 2>/dev/null || true)"
    if [[ -x "${detected_jdk11}/bin/java" ]]; then
        java_command="${detected_jdk11}/bin/java"
    fi
fi

gradle_command=(
    "${java_command}"
    -Dorg.gradle.appname=gradlew
    -classpath "${wrapper_dir}/gradle-wrapper.jar"
    org.gradle.wrapper.GradleWrapperMain
    -p "${project_dir}"
    --no-daemon
    --console=plain
    --stacktrace
    "-Parouter.repository=${local_repository}"
    "-Parouter.annotation.version=${annotation_version}"
    "-Parouter.compiler.version=${compiler_version}"
)

# Prove that this fixture reaches the exact #1035 failure path by running the
# still-published 1.5.2 compiler from Maven Central without the local fix.
published_project_dir="${test_root}/published-project"
empty_repository="${test_root}/empty-maven"
mkdir -p "${published_project_dir}" "${empty_repository}"
rsync -a --exclude '.gradle/' --exclude 'build/' \
    "${fixture_source}/" "${published_project_dir}/"
published_gradle_command=(
    "${java_command}"
    -Dorg.gradle.appname=gradlew
    -classpath "${wrapper_dir}/gradle-wrapper.jar"
    org.gradle.wrapper.GradleWrapperMain
    -p "${published_project_dir}"
    --no-daemon
    --console=plain
    --stacktrace
    "-Parouter.repository=${empty_repository}"
    -Parouter.annotation.version=1.0.6
    -Parouter.compiler.version=1.5.2
)

negative_control_log="${test_root}/published-1.5.2.log"
if "${published_gradle_command[@]}" --refresh-dependencies clean kaptKotlin \
        >"${negative_control_log}" 2>&1; then
    echo "Published ARouter compiler 1.5.2 unexpectedly passed the #1035 fixture." >&2
    exit 1
fi
if ! grep -Fq 'IncrementalFiler.createResource' "${negative_control_log}" || \
        ! grep -Fq 'RouteProcessor.init' "${negative_control_log}"; then
    echo "Published ARouter compiler 1.5.2 did not reproduce the expected #1035 failure." >&2
    sed -n '1,320p' "${negative_control_log}" >&2
    exit 1
fi

dependency_log="${test_root}/dependencies.log"
"${gradle_command[@]}" dependencies --configuration kapt >"${dependency_log}"
if ! grep -Fq 'androidx.room:room-compiler:2.4.3' "${dependency_log}"; then
    echo "KAPT fixture did not resolve Room compiler 2.4.3." >&2
    exit 1
fi

build_log="${test_root}/clean-build.log"
if ! "${gradle_command[@]}" --refresh-dependencies clean build >"${build_log}" 2>&1; then
    sed -n '1,320p' "${build_log}" >&2
    exit 1
fi

generated_root="${project_dir}/build/generated/source/kapt/main"
group_file="${generated_root}/com/alibaba/android/arouter/routes/ARouter\$\$Group\$\$kaptroom.java"
root_file="${generated_root}/com/alibaba/android/arouter/routes/ARouter\$\$Root\$\$kaptroomfixture.java"
provider_file="${generated_root}/com/alibaba/android/arouter/routes/ARouter\$\$Providers\$\$kaptroomfixture.java"
route_doc="${generated_root}/com/alibaba/android/arouter/docs/arouter-map-of-kaptroomfixture.json"

for output in "${group_file}" "${root_file}" "${provider_file}" "${route_doc}"; do
    if [[ ! -f "${output}" ]]; then
        echo "Missing KAPT + Room fixture output: ${output}" >&2
        exit 1
    fi
done

grep -Fq 'atlas.put("/kaptroom/activity",' "${group_file}"
grep -Fq '"path": "/kaptroom/activity"' "${route_doc}"

fixture_jar="${project_dir}/build/libs/arouter-kapt-room-fixture.jar"
if [[ ! -f "${fixture_jar}" ]]; then
    echo "KAPT + Room fixture jar was not assembled." >&2
    exit 1
fi
jar tf "${fixture_jar}" | grep -Fq "ARouter\$\$Group\$\$kaptroom.class"
jar tf "${fixture_jar}" | grep -Fq "ARouter\$\$Root\$\$kaptroomfixture.class"

route_source="${project_dir}/src/main/kotlin/fixture/KaptRoomActivity.kt"
perl -0pi -e 's#/kaptroom/activity#/kaptroom/activity-updated#g' "${route_source}"

incremental_log="${test_root}/incremental-build.log"
if ! "${gradle_command[@]}" build >"${incremental_log}" 2>&1; then
    sed -n '1,320p' "${incremental_log}" >&2
    exit 1
fi
grep -Fq 'atlas.put("/kaptroom/activity-updated",' "${group_file}"
grep -Fq '"path": "/kaptroom/activity-updated"' "${route_doc}"
if grep -Fq 'atlas.put("/kaptroom/activity",' "${group_file}"; then
    echo "Incremental KAPT build retained the stale route path." >&2
    exit 1
fi

no_change_log="${test_root}/no-change-build.log"
"${gradle_command[@]}" build >"${no_change_log}" 2>&1
if ! grep -Fq ':kaptKotlin UP-TO-DATE' "${no_change_log}"; then
    echo "No-change KAPT + Room build was not up-to-date." >&2
    sed -n '1,240p' "${no_change_log}" >&2
    exit 1
fi

echo "ARouter KAPT + Room 2.4.3 regression verification passed."
