package com.github.tarcv.tongs.test.util

val isRunStubbed = System.getenv("CI_STUBBED")?.toBoolean() ?: false
