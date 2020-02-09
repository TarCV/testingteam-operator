#!/usr/bin/env bash

set -ex

cd "$(dirname "$0")"

export GRADLE_OPTS="-Dorg.gradle.console=plain"

# Wait for the output directory to be unlocked after the previous run
sleep 3

rm ./*.log || true
rm -r app/build/reports || true

if [[ ${CI_STUBBED} != 'true' ]]; then
    ./gradlew :app:uninstallF1Debug :app:uninstallF1DebugAndroidTest --stacktrace
    ./gradlew :app:uninstallF2Debug :app:uninstallF2DebugAndroidTest
fi
./gradlew :app:tongsF1DebugAndroidTest --stacktrace
./gradlew :app:testF1DebugUnitTest

rm ./*.log || true
rm -r app/build/reports || true

if [[ ${CI_STUBBED} != 'true' ]]; then
    ./gradlew :app:uninstallF1Debug :app:uninstallF1DebugAndroidTest
    ./gradlew :app:uninstallF2Debug :app:uninstallF2DebugAndroidTest --stacktrace
fi
./gradlew :app:tongsF2DebugAndroidTest --stacktrace
./gradlew :app:testF2DebugUnitTest

rm ./*.log || true
rm -r app/build/reports || true
rm app/tongs.json || true

if [[ ${CI_STUBBED} != 'true' ]]; then
    ./gradlew :app:uninstallF1Debug :app:uninstallF1DebugAndroidTest
    ./gradlew :app:uninstallF2Debug :app:uninstallF2DebugAndroidTest --stacktrace
fi

rm ./*.log || true
rm -r app/build/reports || true
rm app/tongs.json || true

cp app/tongs.sample.json app/tongs.json
TEST_ROOT="$(pwd)"
TEST_ROOT=$(echo "$TEST_ROOT" | perl -lape 's/^\/([a-z])\//$1:\//g')
SDK_PATH=$(echo "$ANDROID_HOME" | sed 's#\\#/#' | perl -lape 's/^\/([a-z])\//$1:\//g')
sed -i.bak "s/DEVICE1/${DEVICE1}/" app/tongs.json
sed -i.bak "s/DEVICE2/${DEVICE2}/" app/tongs.json
sed -i.bak "s#TEST_ROOT#${TEST_ROOT}#" app/tongs.json
if [[ ${CI_STUBBED} != 'true' ]]; then
    sed -i.bak "s/TEST_RUN_TYPE/RECORD_LISTENER_EVENTS/" app/tongs.json
else
    sed -i.bak "s/TEST_RUN_TYPE/STUB_PARALLEL_TESTRUN/" app/tongs.json
fi

pushd ../../plugin
    ./gradlew :tongs-runner:run --stacktrace -PworkingDir="$TEST_ROOT" -Pargs="--sdk $SDK_PATH --apk $TEST_ROOT/app/build/outputs/apk/f2/debug/app-f2-universal-debug.apk --test-apk $TEST_ROOT/app/build/outputs/apk/androidTest/f2/debug/app-f2-debug-androidTest.apk --config $TEST_ROOT/app/tongs.json"
popd
./gradlew :app:testF2DebugUnitTest
