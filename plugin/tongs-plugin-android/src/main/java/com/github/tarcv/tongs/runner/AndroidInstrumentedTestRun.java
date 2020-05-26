/*
 * Copyright 2020 TarCV
 * Copyright 2016 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs.runner;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.github.tarcv.tongs.api.result.TestCaseRunResult;
import com.github.tarcv.tongs.api.testcases.TestCase;
import com.github.tarcv.tongs.api.run.TestCaseEvent;
import com.github.tarcv.tongs.runner.listeners.BaseListener;
import com.github.tarcv.tongs.runner.listeners.IResultProducer;
import com.github.tarcv.tongs.system.io.RemoteFileManager;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static java.lang.String.format;

public class AndroidInstrumentedTestRun {
	private static final Logger logger = LoggerFactory.getLogger(AndroidInstrumentedTestRun.class);
	private static final String TESTCASE_FILTER = "com.github.tarcv.tongs.ondevice.ClassMethodFilter";
	public static final String COLLECTING_RUN_FILTER = "com.github.tarcv.tongs.ondevice.AnnontationReadingFilter";
	private final String poolName;
	private final TestRunParameters testRunParameters;
	private final List<BaseListener> testRunListeners;
	private final IRemoteAndroidTestRunnerFactory remoteAndroidTestRunnerFactory;
	private final IResultProducer resultProducer;

	public AndroidInstrumentedTestRun(String poolName,
                                      TestRunParameters testRunParameters,
                                      List<BaseListener> testRunListeners,
                                      IResultProducer resultProducer,
                                      IRemoteAndroidTestRunnerFactory remoteAndroidTestRunnerFactory) {
        this.poolName = poolName;
		this.testRunParameters = testRunParameters;
		this.testRunListeners = testRunListeners;
		this.resultProducer = resultProducer;
		this.remoteAndroidTestRunnerFactory = remoteAndroidTestRunnerFactory;
	}

	public TestCaseRunResult execute() {
		String applicationPackage = testRunParameters.getApplicationPackage();
		String testPackage = testRunParameters.getTestPackage();
		IDevice device = testRunParameters.getDeviceInterface();

		RemoteAndroidTestRunner runner =
				remoteAndroidTestRunnerFactory.createRemoteAndroidTestRunner(testPackage, testRunParameters.getTestRunner(), device);

		runner.setRunName(poolName);
		runner.setMaxtimeToOutputResponse(testRunParameters.getTestOutputTimeout());

		// Custom filter is required to support Parameterized tests with default names
		TestCaseEvent test = testRunParameters.getTest();
		String testClassName;
		String testMethodName;
		TestCase testCase;
		if (test != null) {
			testClassName = test.getTestClass();
			testMethodName = test.getTestMethod();
			testCase = new TestCase(testMethodName, testClassName);

			String encodedClassName = remoteAndroidTestRunnerFactory.encodeTestName(testClassName);
			String encodedMethodName = remoteAndroidTestRunnerFactory.encodeTestName(testMethodName);

			remoteAndroidTestRunnerFactory.properlyAddInstrumentationArg(runner, "tongs_filterClass", encodedClassName);
			remoteAndroidTestRunnerFactory.properlyAddInstrumentationArg(runner, "tongs_filterMethod", encodedMethodName);

			addFilterAndCustomArgs(runner, TESTCASE_FILTER);

			if (testRunParameters.isCoverageEnabled()) {
				runner.setCoverage(true);
				runner.addInstrumentationArg("coverageFile", RemoteFileManager.getCoverageFileName(testCase));
			}
		} else {
			testClassName = "Test case collection";
			testMethodName = "";
			testCase = new TestCase(testMethodName, testClassName);

			runner.addBooleanArg("log", true);
			addFilterAndCustomArgs(runner, COLLECTING_RUN_FILTER);
		}

		String excludedAnnotation = testRunParameters.getExcludedAnnotation();
		if (!Strings.isNullOrEmpty(excludedAnnotation)) {
			logger.info("Tests annotated with {} will be excluded", excludedAnnotation);
			runner.addInstrumentationArg("notAnnotation", excludedAnnotation);
		} else {
			logger.info("No excluding any test based on annotations");
		}

		try {
			logger.info("Cmd: " + runner.getAmInstrumentCommand());
			runner.run(testRunListeners.toArray(new ITestRunListener[0]));
		} catch (ShellCommandUnresponsiveException | TimeoutException e) {
			logger.warn("Test: " + testClassName + " got stuck. You can increase the timeout in settings if it's too strict");
		} catch (AdbCommandRejectedException | IOException e) {
			throw new RuntimeException(format("Error while running test %s %s", testClassName, testMethodName), e);
		}

        return resultProducer.getResult();
    }

	private void addFilterAndCustomArgs(RemoteAndroidTestRunner runner, String collectingRunFilter) {
		testRunParameters.getTestRunnerArguments().entrySet().stream()
				.filter(nameValue -> !nameValue.getKey().equals("filter"))
				.filter(nameValue -> !nameValue.getKey().startsWith("tongs_"))
				.forEach(nameValue -> {
					remoteAndroidTestRunnerFactory.properlyAddInstrumentationArg(runner,
							nameValue.getKey(), nameValue.getValue());
				});

		@Nullable String customFilters = testRunParameters.getTestRunnerArguments().get("filter");
		String filters;
		if (customFilters != null) {
			filters = customFilters + "," + collectingRunFilter;
		} else {
			filters = collectingRunFilter;
		}

		runner.addInstrumentationArg("filter", filters);
	}
}
