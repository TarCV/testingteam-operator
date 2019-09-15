package com.github.tarcv.tongs.runner;

public class TestCaseFailed implements TestCaseRunResult {
    private final String stackTrace;

    public TestCaseFailed() {
        this("");
    }

    public TestCaseFailed(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getStackTrace() {
        return stackTrace;
    }
}
