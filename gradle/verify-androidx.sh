#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"
for tool in rg unzip strings; do
    command -v "${tool}" >/dev/null || { echo "Missing required tool: ${tool}" >&2; exit 1; }
done
dependency_log="$(mktemp "${TMPDIR:-/tmp}/arouter-androidx-dependencies.XXXXXX")"
classes_jar="$(mktemp "${TMPDIR:-/tmp}/arouter-androidx-classes.XXXXXX")"
class_strings="$(mktemp "${TMPDIR:-/tmp}/arouter-androidx-strings.XXXXXX")"

cleanup() {
    rm -f -- "${dependency_log}" "${classes_jar}" "${class_strings}"
}
trap cleanup EXIT

cd "${repo_root}"

# The compiler intentionally retains string-only detection for legacy Fragment
# sources so it can still be paired with the 1.x runtime. Runtime artifacts and
# demos must be native AndroidX, which is the scope checked below.
runtime_paths=(
    README.md
    README_CN.md
    app
    arouter-api
    gradle/configuration-cache-fixture
    gradle.properties
    module-java
    module-java-export
    module-kotlin
)

if rg --line-number \
    --glob '!**/build/**' \
    --glob '!**/.gradle/**' \
    'android\.support|com\.android\.support' \
    "${runtime_paths[@]}" >"${dependency_log}"; then
    echo "Legacy Support Library references remain in AndroidX runtime sources:" >&2
    cat "${dependency_log}" >&2
    exit 1
else
    scan_status=$?
    if [[ ${scan_status} -ne 1 ]]; then
        echo "Failed to scan AndroidX runtime sources (exit ${scan_status})." >&2
        exit "${scan_status}"
    fi
fi

grep -Fxq 'android.useAndroidX=true' gradle.properties
grep -Fxq 'android.enableJetifier=false' gradle.properties
grep -Fxq 'android.useAndroidX=true' gradle/configuration-cache-fixture/gradle.properties
grep -Fxq 'android.enableJetifier=false' gradle/configuration-cache-fixture/gradle.properties

./gradlew "$@" --no-daemon --console=plain -q -Parouter.useLocalRegisterPlugin \
    :app:dependencies --configuration debugRuntimeClasspath >"${dependency_log}"

if grep -Eq '(^|[[:space:]])FAILED([[:space:]]|$)' "${dependency_log}"; then
    echo "The AndroidX dependency graph contains unresolved dependencies:" >&2
    cat "${dependency_log}" >&2
    exit 1
fi

if grep -Fq 'com.android.support' "${dependency_log}"; then
    echo "Legacy Support Library artifacts remain on the demo runtime classpath:" >&2
    grep -F 'com.android.support' "${dependency_log}" >&2
    exit 1
fi

for dependency in \
    'androidx.core:core:' \
    'androidx.fragment:fragment:' \
    'androidx.appcompat:appcompat:' \
    'androidx.constraintlayout:constraintlayout:'; do
    if ! grep -Fq "${dependency}" "${dependency_log}"; then
        echo "Missing AndroidX runtime dependency: ${dependency}" >&2
        exit 1
    fi
done

api_version="$(awk -F= '$1 == "VERSION_NAME" { print $2; exit }' arouter-api/gradle.properties)"
published_dir="${repo_root}/build/localMaven/com/alibaba/arouter-api/${api_version}"
published_pom="${published_dir}/arouter-api-${api_version}.pom"
published_aar="${published_dir}/arouter-api-${api_version}.aar"

for artifact in "${published_pom}" "${published_aar}"; do
    if [[ ! -s "${artifact}" ]]; then
        echo "Missing local ARouter API artifact: ${artifact}" >&2
        echo "Run :arouter-api:installLocally before this verifier." >&2
        exit 1
    fi
done

if [[ arouter-api/build.gradle -nt "${published_pom}" \
        || gradle.properties -nt "${published_pom}" ]]; then
    echo "Local ARouter API publication is older than the AndroidX build configuration." >&2
    echo "Run :arouter-api:installLocally before this verifier." >&2
    exit 1
fi

if grep -Fq '<groupId>com.android.support</groupId>' "${published_pom}"; then
    echo "Published ARouter API POM still exposes the legacy Support Library." >&2
    exit 1
fi

for group_id in androidx.core androidx.fragment; do
    if ! grep -Fq "<groupId>${group_id}</groupId>" "${published_pom}"; then
        echo "Published ARouter API POM is missing ${group_id}." >&2
        exit 1
    fi
done

if ! awk '
    /<dependency>/ { block = "" }
    { block = block $0 "\n" }
    /<\/dependency>/ {
        if (index(block, "<groupId>androidx.core</groupId>") &&
                index(block, "<scope>compile</scope>")) {
            found = 1
        }
        block = ""
    }
    END { exit found ? 0 : 1 }
' "${published_pom}"; then
    echo "Published ARouter API POM does not expose AndroidX Core at compile scope." >&2
    exit 1
fi

unzip -p "${published_aar}" classes.jar >"${classes_jar}"
unzip -p "${classes_jar}" | strings >"${class_strings}"

if grep -Fq 'android/support/' "${class_strings}"; then
    echo "Published ARouter API bytecode still references android.support classes." >&2
    grep -F 'android/support/' "${class_strings}" >&2
    exit 1
fi

for class_name in \
    'androidx/core/app/ActivityOptionsCompat' \
    'androidx/fragment/app/Fragment'; do
    if ! grep -Fq "${class_name}" "${class_strings}"; then
        echo "Published ARouter API bytecode is missing ${class_name}." >&2
        exit 1
    fi
done

echo "ARouter native AndroidX dependency verification passed with Jetifier disabled."
