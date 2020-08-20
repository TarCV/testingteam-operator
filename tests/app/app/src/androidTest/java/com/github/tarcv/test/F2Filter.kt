package com.github.tarcv.test

import org.junit.runner.Description
import org.junit.runner.manipulation.Filter

class F2Filter : Filter() {
    override fun shouldRun(description: Description): Boolean {
        val filteredOutTest = description.methodName?.startsWith("filteredByF2Filter") == true
        return !filteredOutTest
    }

    override fun describe(): String = "Filter out filteredByF2Filter test case"
}