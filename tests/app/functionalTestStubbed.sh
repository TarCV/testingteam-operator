#!/usr/bin/env bash

set -e

echo "Stubbed functional tests are not supported for now"
exit 1

export DEVICE1="tongs-5554"
export DEVICE2="tongs-5556"
export CI_STUBBED=true
"$(dirname "$0")"/functionalTestCustom.sh
