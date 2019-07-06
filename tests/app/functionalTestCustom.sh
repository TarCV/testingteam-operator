#!/usr/bin/env bash

set -ex

cd `dirname "$0"`

export GRADLE_OPTS="-Dorg.gradle.console=plain"

./gradlew --stop
sleep 3

rm *.log || true
rm -r app/build/reports || true

if [[ ${CI_STUBBED} != 'true' ]]; then
    ./gradlew :app:uninstallF1Debug :app:uninstallF1DebugAndroidTest --stacktrace
    ./gradlew :app:uninstallF2Debug :app:uninstallF2DebugAndroidTest
fi
./gradlew :app:tongsF1DebugAndroidTest --stacktrace
./gradlew :app:testF1DebugUnitTest

rm *.log || true
rm -r app/build/reports || true

if [[ ${CI_STUBBED} != 'true' ]]; then
    ./gradlew :app:uninstallF1Debug :app:uninstallF1DebugAndroidTest
    ./gradlew :app:uninstallF2Debug :app:uninstallF2DebugAndroidTest --stacktrace
fi
./gradlew :app:tongsF2DebugAndroidTest --stacktrace
./gradlew :app:testF2DebugUnitTest