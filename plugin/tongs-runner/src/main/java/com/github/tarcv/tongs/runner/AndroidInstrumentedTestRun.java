/*
 * Copyright 2019 TarCV
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
import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.github.tarcv.tongs.model.TestCaseEvent;
import com.github.tarcv.tongs.runner.listeners.BaseListener;
import com.github.tarcv.tongs.system.PermissionGrantingManager;
import com.github.tarcv.tongs.system.io.RemoteFileManager;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

public class AndroidInstrumentedTestRun {
	private static final Logger logger = LoggerFactory.getLogger(AndroidInstrumentedTestRun.class);
    private final String poolName;
	private final TestRunParameters testRunParameters;
	private final List<BaseListener> testRunListeners;
	private final List<TestRule> testRules;
	private final PermissionGrantingManager permissionGrantingManager;
	private final IRemoteAndroidTestRunnerFactory remoteAndroidTestRunnerFactory;

	public AndroidInstrumentedTestRun(String poolName,
									  TestRunParameters testRunParameters,
									  List<BaseListener> testRunListeners,
									  List<TestRule> testRules,
									  PermissionGrantingManager permissionGrantingManager,
									  IRemoteAndroidTestRunnerFactory remoteAndroidTestRunnerFactory) {
        this.poolName = poolName;
		this.testRunParameters = testRunParameters;
		this.testRunListeners = testRunListeners;
		this.testRules = testRules;
		this.permissionGrantingManager = permissionGrantingManager;
		this.remoteAndroidTestRunnerFactory = remoteAndroidTestRunnerFactory;
	}

	public void execute() {
		String applicationPackage = testRunParameters.getApplicationPackage();
		String testPackage = testRunParameters.getTestPackage();
		IDevice device = testRunParameters.getDeviceInterface();

		RemoteAndroidTestRunner runner =
				remoteAndroidTestRunnerFactory.createRemoteAndroidTestRunner(testPackage, testRunParameters.getTestRunner(), device);

		IRemoteAndroidTestRunner.TestSize testSize = testRunParameters.getTestSize();
		if (testSize != null) {
			runner.setTestSize(testSize);
		}
		runner.setRunName(poolName);
		runner.setMaxtimeToOutputResponse(testRunParameters.getTestOutputTimeout());

		// Custom filter is required to support Parameterized tests with default names
		TestCaseEvent test = testRunParameters.getTest();
		String testClassName;
		String testMethodName;
		if (test != null) {
			testClassName = test.getTestClass();
			testMethodName = test.getTestMethod();
			runner.addInstrumentationArg("filter", "com.github.tarcv.tongs.ondevice.ClassMethodFilter");
			remoteAndroidTestRunnerFactory.properlyAddInstrumentationArg(runner, "filterClass", testClassName);
			remoteAndroidTestRunnerFactory.properlyAddInstrumentationArg(runner, "filterMethod", testMethodName);

			if (testRunParameters.isCoverageEnabled()) {
				runner.setCoverage(true);
				runner.addInstrumentationArg("coverageFile", RemoteFileManager.getCoverageFileName(new TestIdentifier(testClassName, testMethodName)));
			}
		} else {
			testClassName = "Test case collection";
			testMethodName = "";
			runner.addBooleanArg("log", true);
			runner.addInstrumentationArg("filter", "com.github.tarcv.tongs.ondevice.AnnontationReadingFilter");
		}

		String excludedAnnotation = testRunParameters.getExcludedAnnotation();
		if (!Strings.isNullOrEmpty(excludedAnnotation)) {
			logger.info("Tests annotated with {} will be excluded", excludedAnnotation);
			runner.addInstrumentationArg("notAnnotation", excludedAnnotation);
		} else {
			logger.info("No excluding any test based on annotations");
		}

		testRules.forEach(TestRule::before);

		List<String> permissionsToGrant;
		if (test != null) {
			// TODO: Implement as a TestRule
			permissionsToGrant = test.getPermissionsToGrant();
			permissionGrantingManager.grantPermissions(applicationPackage, device, permissionsToGrant);
			permissionGrantingManager.grantPermissions(testPackage, device, permissionsToGrant);
		} else {
			permissionsToGrant = Collections.emptyList();
		}

		try {
			logger.info("Cmd: " + runner.getAmInstrumentCommand());
			runner.run(testRunListeners.toArray(new ITestRunListener[0]));
		} catch (ShellCommandUnresponsiveException | TimeoutException e) {
			logger.warn("Test: " + testClassName + " got stuck. You can increase the timeout in settings if it's too strict");
		} catch (AdbCommandRejectedException | IOException e) {
			throw new RuntimeException(format("Error while running test %s %s", testClassName, testMethodName), e);
		} finally {
			if (test != null) {
				permissionGrantingManager.revokePermissions(applicationPackage, device, permissionsToGrant);
			}

			testRules.forEach(TestRule::after);
		}

    }
}
