/*
 * Copyright 2018 TarCV
 * Copyright 2014 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs.runner;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;
import com.github.tarcv.tongs.model.TestCaseEvent;

import javax.annotation.Nullable;
import java.util.Map;

public class TestRunParameters {
	private final TestCaseEvent test;
	private final String testPackage;
	private final String testRunner;
	private final Map<String, String> testRunnerArguments;
	private final boolean isCoverageEnabled;
	private final IRemoteAndroidTestRunner.TestSize testSize;
	private final int testOutputTimeout;
	private final IDevice deviceInterface;
	private final String excludedAnnotation;
	private final String applicationPackage;

	public TestCaseEvent getTest() {
		return test;
	}

	public String getTestPackage() {
		return testPackage;
	}

	public String getTestRunner() {
		return testRunner;
	}

	public Map<String, String> getTestRunnerArguments() {
		return testRunnerArguments;
	}

	@Nullable
	public IRemoteAndroidTestRunner.TestSize getTestSize() {
		return testSize;
	}

	public int getTestOutputTimeout() {
		return testOutputTimeout;
	}

	public IDevice getDeviceInterface() {
		return deviceInterface;
	}

	public boolean isCoverageEnabled(){
		return isCoverageEnabled;
	}

	public String getExcludedAnnotation() {
		return excludedAnnotation;
	}

	public String getApplicationPackage() {
		return applicationPackage;
	}

	public static class Builder {
		private TestCaseEvent test;
		private String testPackage;
		private String testRunner;
		private Map<String, String> testRunnerArguments;
		private boolean isCoverageEnabled;
		private IRemoteAndroidTestRunner.TestSize testSize;
		private IDevice deviceInterface;
		private int testOutputTimeout;
		private String excludedAnnotation;
		private String applicationPackage;

		public static Builder testRunParameters() {
			return new Builder();
		}

		public Builder withTest(TestCaseEvent test) {
			this.test = test;
			return this;
		}

		public Builder withTestPackage(String testPackage) {
			this.testPackage = testPackage;
			return this;
		}

		public Builder withTestRunner(String testRunner) {
			this.testRunner = testRunner;
			return this;
		}

		public Builder withTestSize(IRemoteAndroidTestRunner.TestSize testSize) {
			this.testSize = testSize;
			return this;
		}

		public Builder withTestOutputTimeout(int testOutputTimeout) {
			this.testOutputTimeout = testOutputTimeout;
			return this;
		}

		public Builder withDeviceInterface(IDevice deviceInterface) {
			this.deviceInterface = deviceInterface;
			return this;
		}

		public Builder withCoverageEnabled(boolean isCoverageEnabled){
			this.isCoverageEnabled = isCoverageEnabled;
			return this;
		}

		public Builder withExcludedAnnotation(String excludedAnnotation) {
			this.excludedAnnotation = excludedAnnotation;
			return this;
		}

		public Builder withApplicationPackage(String applicationPackage) {
			this.applicationPackage = applicationPackage;
			return this;
		}

		public Builder withTestRunnerArguments(Map<String, String> testRunnerArguments) {
			this.testRunnerArguments = testRunnerArguments;
			return this;
		}

		public TestRunParameters build() {
			return new TestRunParameters(this);
		}
	}

	private TestRunParameters(Builder builder) {
		test = builder.test;
		testPackage = builder.testPackage;
		testRunner = builder.testRunner;
		testRunnerArguments = builder.testRunnerArguments;
		testSize = builder.testSize;
		testOutputTimeout = builder.testOutputTimeout;
		deviceInterface = builder.deviceInterface;
		isCoverageEnabled = builder.isCoverageEnabled;
		this.excludedAnnotation = builder.excludedAnnotation;
		this.applicationPackage = builder.applicationPackage;
	}
}
