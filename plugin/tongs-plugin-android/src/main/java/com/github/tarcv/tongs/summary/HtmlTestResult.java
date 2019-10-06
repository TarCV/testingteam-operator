/*
 * Copyright 2019 TarCV
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
package com.github.tarcv.tongs.summary;

import com.github.tarcv.tongs.model.TestCaseEvent;

import java.util.Collection;

/**
 * Plain bean class, to feed to Moustache markup files.
 */
public class HtmlTestResult {
	public String status;
	public String prettyClassName;
	public String prettyMethodName;
	public String deviceSerial;
	public String deviceSafeSerial;
	public String deviceModelDespaced;
	public TestCaseEvent testIdentifier;
	public String fileNameForTest;
	public String poolName;
	public Collection<HtmlLogCatMessage> logcatMessages;
	public String timeTaken;
	public String[] trace;
    public boolean diagnosticVideo;
    public boolean diagnosticScreenshots;
}
