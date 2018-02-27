/*
 * Copyright 2018 TarCV
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
import com.android.ddmlib.NullOutputReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.google.common.base.Strings;
import com.github.tarcv.tongs.model.TestCaseEvent;
import com.github.tarcv.tongs.system.PermissionGrantingManager;
import com.github.tarcv.tongs.system.io.RemoteFileManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static java.lang.String.format;

class TestRun {
	private static final Logger logger = LoggerFactory.getLogger(TestRun.class);
    private final String poolName;
	private final TestRunParameters testRunParameters;
	private final List<ITestRunListener> testRunListeners;
	private final PermissionGrantingManager permissionGrantingManager;
	private final IRemoteAndroidTestRunnerFactory remoteAndroidTestRunnerFactory;

	public TestRun(String poolName,
				   TestRunParameters testRunParameters,
				   List<ITestRunListener> testRunListeners,
				   PermissionGrantingManager permissionGrantingManager,
				   IRemoteAndroidTestRunnerFactory remoteAndroidTestRunnerFactory) {
        this.poolName = poolName;
		this.testRunParameters = testRunParameters;
		this.testRunListeners = testRunListeners;
		this.permissionGrantingManager = permissionGrantingManager;
		this.remoteAndroidTestRunnerFactory = remoteAndroidTestRunnerFactory;
	}

	public void execute() {
		String applicationPackage = testRunParameters.getApplicationPackage();
		IDevice device = testRunParameters.getDeviceInterface();

		RemoteAndroidTestRunner runner =
				remoteAndroidTestRunnerFactory.createRemoteAndroidTestRunner(testRunParameters.getTestPackage(), testRunParameters.getTestRunner(), device);

		TestCaseEvent test = testRunParameters.getTest();
		String testClassName = test.getTestClass();
		String testMethodName = test.getTestMethod();
		IRemoteAndroidTestRunner.TestSize testSize = testRunParameters.getTestSize();
		if (testSize != null) {
			runner.setTestSize(testSize);
		}
		runner.setRunName(poolName);
		runner.setMaxtimeToOutputResponse(testRunParameters.getTestOutputTimeout());

		// Custom filter is required to support Parameterized tests with default names
		runner.addInstrumentationArg("filter", "com.github.tarcv.tongs.ondevice.ClassMethodFilter");
		remoteAndroidTestRunnerFactory.properlyAddInstrumentationArg(runner, "filterClass", testClassName);
		remoteAndroidTestRunnerFactory.properlyAddInstrumentationArg(runner, "filterMethod", testMethodName);

        if (testRunParameters.isCoverageEnabled()) {
            runner.setCoverage(true);
            runner.addInstrumentationArg("coverageFile", RemoteFileManager.getCoverageFileName(new TestIdentifier(testClassName, testMethodName)));
        }
		String excludedAnnotation = testRunParameters.getExcludedAnnotation();
		if (!Strings.isNullOrEmpty(excludedAnnotation)) {
			logger.info("Tests annotated with {} will be excluded", excludedAnnotation);
			runner.addInstrumentationArg("notAnnotation", excludedAnnotation);
		} else {
			logger.info("No excluding any test based on annotations");
		}

		clearPackageData(device, applicationPackage);
		clearPackageData(device, testRunParameters.getTestPackage());

		List<String> permissionsToRevoke = testRunParameters.getTest().getPermissionsToRevoke();

		permissionGrantingManager.revokePermissions(applicationPackage, device, permissionsToRevoke);

		try {
			logger.error("Cmd: " + runner.getAmInstrumentCommand());
			runner.run(testRunListeners);
		} catch (ShellCommandUnresponsiveException | TimeoutException e) {
			logger.warn("Test: " + testClassName + " got stuck. You can increase the timeout in settings if it's too strict");
		} catch (AdbCommandRejectedException | IOException e) {
			throw new RuntimeException(format("Error while running test %s %s", test.getTestClass(), test.getTestMethod()), e);
		} finally {
			permissionGrantingManager.restorePermissions(applicationPackage, device, permissionsToRevoke);
		}

    }

	private void clearPackageData(IDevice device, String applicationPackage) {
		long start = System.currentTimeMillis();

		try {
			device.executeShellCommand(format("pm clear %s", applicationPackage), new NullOutputReceiver());
		} catch (TimeoutException | AdbCommandRejectedException | ShellCommandUnresponsiveException | IOException e) {
			throw new UnsupportedOperationException(format("Unable to clear package data (%s)", applicationPackage), e);
		}

		logger.debug("Clearing application data: {} (took {}ms)", applicationPackage, (System.currentTimeMillis() - start));
	}
}
