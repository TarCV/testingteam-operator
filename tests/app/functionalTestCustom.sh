#!/usr/bin/env bash

set -ex

cd `dirname "$0"`

export GRADLE_OPTS="-Dorg.gradle.console=plain"

rm *.log || true
./gradlew :app:uninstallF1Debug :app:uninstallF1DebugAndroidTest --stacktrace
./gradlew :app:uninstallF2Debug :app:uninstallF2DebugAndroidTest
./gradlew :app:tongsF1DebugAndroidTest --stacktrace
./gradlew :app:testF1DebugUnitTest

rm *.log || true
./gradlew :app:uninstallF1Debug :app:uninstallF1DebugAndroidTest
./gradlew :app:uninstallF2Debug :app:uninstallF2DebugAndroidTest --stacktrace
./gradlew :app:tongsF2DebugAndroidTest --stacktrace
./gradlew :app:testF2DebugUnitTest
