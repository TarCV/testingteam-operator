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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name="testcase", strict=false)
class TestCase {

	@Attribute
	private String name;

	@Attribute
	private String classname;

	@Attribute
	private float time;

	@Element(required=false)
	private String failure;

	@Element(required=false)
	private String error;

    TestCase() {
    }

	public String getName() {
		return name;
	}

	public String getClassname() {
		return classname;
	}

	public float getTime() {
		return time;
	}

	public String getError() {
		return error;
	}

	public String getFailure() {
		return failure;
	}
}
