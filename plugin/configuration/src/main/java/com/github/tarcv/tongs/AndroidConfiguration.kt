package com.github.tarcv.tongs

class AndroidConfiguration {
    var testRunnerClass: String? = null

    /*
     * Instrumentation runner additional arguments (the ones passed with -e)
     */
    var instrumentationArguments = Defaults.TEST_RUNNER_ARGUMENTS
}