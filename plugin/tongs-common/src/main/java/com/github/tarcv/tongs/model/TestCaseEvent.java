/*
 * Copyright 2019 TarCV
 * Copyright 2018 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.model;

import com.android.ddmlib.testrunner.TestIdentifier;
import com.google.common.base.Objects;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.SIMPLE_STYLE;

public class TestCaseEvent {

    private final String testMethod;
    private final String testClass;
    private final boolean isIgnored;
    private final List<String> permissionsToGrant;
    private final Map<String, String> properties;
    private final HashSet<Device> excludedDevices;

    private TestCaseEvent(String testMethod, String testClass, boolean isIgnored, List<String> permissionsToGrant, Map<String, String> properties, Collection<Device> excludedDevices) {
        this.testMethod = testMethod;
        this.testClass = testClass;
        this.isIgnored = isIgnored;
        this.permissionsToGrant = permissionsToGrant;
        this.properties = properties;
        this.excludedDevices = new HashSet<>(excludedDevices);
    }

    public static TestCaseEvent newTestCase(String testMethod, String testClass, boolean isIgnored, List<String> permissionsToGrant, Map<String, String> properties, JsonObject info, Collection<Device> excludedDevices) {
        return new TestCaseEvent(testMethod, testClass, isIgnored, permissionsToGrant, properties, excludedDevices);
    }

    public static TestCaseEvent newTestCase(@Nonnull TestIdentifier testIdentifier) {
        return newTestCase(testIdentifier, false);
    }

    public static TestCaseEvent newTestCase(@Nonnull TestIdentifier testIdentifier, boolean isIgnored) {
        return new TestCaseEvent(testIdentifier.getTestName(), testIdentifier.getClassName(), isIgnored,
                emptyList(), emptyMap(), emptyList());
    }

    public boolean isExcluded(Device device) {
        return excludedDevices.contains(device);
    }

    public String getTestMethod() {
        return testMethod;
    }

    public String getTestClass() {
        return testClass;
    }

    public boolean isIgnored() {
        return isIgnored;
    }

    public List<String> getPermissionsToGrant() {
        return permissionsToGrant;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.testMethod, this.testClass);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TestCaseEvent other = (TestCaseEvent) obj;
        return Objects.equal(this.testMethod, other.testMethod)
                && Objects.equal(this.testClass, other.testClass);
    }

    @Override
    public String toString() {
        return reflectionToString(this, SIMPLE_STYLE);
    }
}
