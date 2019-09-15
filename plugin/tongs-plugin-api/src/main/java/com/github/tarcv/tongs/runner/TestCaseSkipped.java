package com.github.tarcv.tongs.runner;

public class TestCaseSkipped implements TestCaseRunResult {
    private final String stackTrace;

    public TestCaseSkipped() {
        this("");
    }

    public TestCaseSkipped(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getStackTrace() {
        return stackTrace;
    }
}
