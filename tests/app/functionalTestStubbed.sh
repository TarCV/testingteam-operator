#!/usr/bin/env bash

set -e

export DEVICE1="tongs-5554"
export DEVICE2="tongs-5556"
export CI_STUBBED=true
"$(dirname "$0")"/functionalTestCustom.sh
