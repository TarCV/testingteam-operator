#!/usr/bin/env bash

set -e

cd `dirname "$0"`

export DEVICE1="tongs-5554"
export DEVICE2="tongs-5556"
export CI_STUBBED=true
./functionalTestCustom.sh
