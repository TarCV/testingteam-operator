/*
 * Copyright 2019 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs.runner

import com.android.ddmlib.IDevice
import com.android.ddmlib.NullOutputReceiver
import com.android.ddmlib.testrunner.ITestRunListener
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner
import com.android.ddmlib.testrunner.TestIdentifier
import com.github.tarcv.tongs.utils.DdmsUtils
import com.github.tarcv.tongs.utils.DdmsUtils.unescapeInstrumentationArg

interface IRemoteAndroidTestRunnerFactory {
    fun createRemoteAndroidTestRunner(testPackage: String, testRunner: String, device: IDevice): RemoteAndroidTestRunner
    fun properlyAddInstrumentationArg(runner: RemoteAndroidTestRunner, name: String, value: String)
}

class RemoteAndroidTestRunnerFactory : IRemoteAndroidTestRunnerFactory {
    override fun createRemoteAndroidTestRunner(testPackage: String, testRunner: String, device: IDevice): RemoteAndroidTestRunner {
        return RemoteAndroidTestRunner(
                testPackage,
                testRunner,
                device)
    }

    override fun properlyAddInstrumentationArg(runner: RemoteAndroidTestRunner, name: String, value: String) {
        DdmsUtils.properlyAddInstrumentationArg(runner, name, value)
    }
}

class TestAndroidTestRunnerFactory : IRemoteAndroidTestRunnerFactory {
    override fun createRemoteAndroidTestRunner(testPackage: String, testRunner: String, device: IDevice): RemoteAndroidTestRunner {
        return object : RemoteAndroidTestRunner(
                testPackage,
                testRunner,
                device) {
            override fun run(listenersCollection: Collection<ITestRunListener>) {
                // Do not call super.run to avoid compile errors cause by SDK tools version incompatibilities
                device.executeShellCommand(amInstrumentCommand, NullOutputReceiver())
                stubbedRun(listenersCollection)
            }

            private fun stubbedRun(listenersCollection: Collection<ITestRunListener>) {
                val listeners = BroadcastingListener(listenersCollection)

                operator fun Regex.contains(other: String): Boolean = this.matches(other)
                val command = amInstrumentCommand
                when (command) {
                    in logOnlyCommandPattern -> {
                        listeners.testRunStarted("emulators", testCases.size)
                        testCases.forEach {
                            listeners.fireTest(it)
                        }
                        listeners.testRunEnded(100, emptyMap())
                    }
                    in testCaseCommandPattern -> {
                        val (testMethod, testClass) =
                                testCaseCommandPattern.matchEntire(command)
                                        ?.groupValues
                                        ?.drop(1) // group 0 is the entire match
                                        ?.map { unescapeInstrumentationArg(it) }
                                        ?: throw IllegalStateException()
                        listeners.testRunStarted("emulators", 1)
                        listeners.fireTest("$testClass#$testMethod", functionalTestTestcaseDuration)
                        listeners.testRunEnded(functionalTestTestcaseDuration, emptyMap())
                    }
                    else -> throw IllegalStateException("Unexpected command: $command")
                }
            }
        }
    }

    override fun properlyAddInstrumentationArg(runner: RemoteAndroidTestRunner, name: String, value: String) {
        // proper escaping really complicates RemoteAndroidTestRunner#stubbedRun implementation
        //  so skip it here
        runner.addInstrumentationArg(name, value)
    }

    private fun ITestRunListener.fireTest(testCase: String, delayMillis: Long = 0) {
        val (className, testName) = testCase.split("#", limit = 2)
        testStarted(TestIdentifier(className, testName))
        if (delayMillis > 0) {
            Thread.sleep(delayMillis)
        }
        testEnded(TestIdentifier(className, testName), emptyMap())
    }

    companion object {
        const val functionalTestTestcaseDuration = 2345L

        private const val expectedTestPackage = "com.github.tarcv.tongstestapp.test"
        private const val expectedTestRunner = "android.support.test.runner.AndroidJUnitRunner"
        val logOnlyCommandPattern =
                ("am\\s+instrument\\s+-w\\s+-r\\s+" +
                        " -e package com.github.tarcv.test" +
                        " -e filter com.github.tarcv.tongs.ondevice.AnnontationReadingFilter" +
                        " -e log true" +
                        """\s+$expectedTestPackage\/$expectedTestRunner""")
                        .replace(".", "\\.")
                        .replace(" -", "\\s+-")
                        .toRegex()
        val testCaseCommandPattern =
                ("am\\s+instrument\\s+-w\\s+-r\\s+" +
                        " -e filterMethod ()" +
                        " -e filter com.github.tarcv.tongs.ondevice.ClassMethodFilter" +
                        " -e filterClass ()" +
                        """\s+$expectedTestPackage\/$expectedTestRunner""")
                        .replace(".", "\\.")
                        .replace(" -", "\\s+-")
                        .replace("()", "(.+?)")
                        .toRegex()
        private val testCases = listOf("""com.github.tarcv.test.DangerousNamesTest#test[param = """ + '$' + """THIS_IS_NOT_A_VAR]""",
                """com.github.tarcv.test.DangerousNamesTest#test[param =        1       ]""",
                """com.github.tarcv.test.DangerousNamesTest#test[param = #######]""",
                """com.github.tarcv.test.DangerousNamesTest#test[param = !!!!!!!]""",
                """com.github.tarcv.test.DangerousNamesTest#test[param = ''''''']""",
                "com.github.tarcv.test.DangerousNamesTest#test[param = \"\"\"\"\"\"\"\"]",
                """com.github.tarcv.test.DangerousNamesTest#test[param = ()$(echo)`echo`()$(echo)`echo`()$(echo)`echo`()$(echo)`echo`()$(echo)`echo`()$(echo)`echo`()$(echo)`echo`]""",
                """com.github.tarcv.test.DangerousNamesTest#test[param = * *.* * *.* * *.* * *.* * *.* * *.* * *.* * *.* *]""",
                """com.github.tarcv.test.DangerousNamesTest#test[param = . .. . .. . .. . .. . .. . .. . .. . .. . .. . ..]""",
                """com.github.tarcv.test.DangerousNamesTest#test[param = |&;<>()${'$'}`?[]#~=%|&;<>()${'$'}`?[]#~=%|&;<>()${'$'}`?[]#~=%|&;<>()${'$'}`?[]#~=%|&;<>()${'$'}`?[]#~=%|&;<>()${'$'}`?[]#~=%|&;<>()${'$'}`?[]#~=%]""",
                """com.github.tarcv.test.DangerousNamesTest#test[param = ; function {}; while {}; for {}; do {}; done {}; exit]""",
                """com.github.tarcv.test.GrantPermissionsForClassTest#testPermissionGranted1""",
                """com.github.tarcv.test.GrantPermissionsForClassTest#testPermissionGranted2""",
                """com.github.tarcv.test.GrantPermissionsForInheritedClassTest#testPermissionGranted1""",
                """com.github.tarcv.test.GrantPermissionsForInheritedClassTest#testPermissionGranted2""",
                """com.github.tarcv.test.GrantPermissionsTest#testPermissionGranted""",
                """com.github.tarcv.test.GrantPermissionsTest#testNoPermissionByDefault""",
                """com.github.tarcv.test.NoPermissionsForOverridesTest#testNoPermissionForOverrides""",
                """com.github.tarcv.test.NormalTest#test""",
                """com.github.tarcv.test.ParameterizedNamedTest#test[param = 1]""",
                """com.github.tarcv.test.ParameterizedNamedTest#test[param = 2]""",
                """com.github.tarcv.test.ParameterizedNamedTest#test[param = 3]""",
                """com.github.tarcv.test.ParameterizedNamedTest#test[param = 4]""",
                """com.github.tarcv.test.ParameterizedNamedTest#test[param = 5]""",
                """com.github.tarcv.test.ParameterizedNamedTest#test[param = 6]""",
                """com.github.tarcv.test.ParameterizedNamedTest#test[param = 7]""",
                """com.github.tarcv.test.ParameterizedNamedTest#test[param = 8]""",
                """com.github.tarcv.test.ParameterizedTest#test[1]""",
                """com.github.tarcv.test.ParameterizedTest#test[2]""",
                """com.github.tarcv.test.ParameterizedTest#test[3]""",
                """com.github.tarcv.test.ParameterizedTest#test[4]""",
                """com.github.tarcv.test.ParameterizedTest#test[5]""",
                """com.github.tarcv.test.ParameterizedTest#test[6]""",
                """com.github.tarcv.test.ParameterizedTest#test[7]""",
                """com.github.tarcv.test.ParameterizedTest#test[8]""",
                """com.github.tarcv.test.PropertiesTest#normalPropertiesTest""",
                """com.github.tarcv.test.PropertiesTest#normalPropertyPairsTest""",
                """com.github.tarcv.test.ResetPrefsTest#testPrefsAreClearedBetweenTests[0]""",
                """com.github.tarcv.test.ResetPrefsTest#testPrefsAreClearedBetweenTests[1]""",
                """com.github.tarcv.test.ResetPrefsTest#testPrefsAreClearedBetweenTests[2]""",
                """com.github.tarcv.test.ResetPrefsTest#testPrefsAreClearedBetweenTests[3]"""
        )

        val logcatLines = listOf("[ 01-02 00:01:02.352 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000000:{"testClass":"com.github.tarcv.test.DangerousNamesTest","testMethod":"test[param = ${'$'}THIS_IS_NOT_A_VAR]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.353 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000001:{"testClass":"com.github.tarcv.test.DangerousNamesTest","testMethod":"test[param =        1       ]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.354 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000002:{"testClass":"com.github.tarcv.test.DangerousNamesTest","testMethod":"test[param = #######]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.354 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000003:{"testClass":"com.github.tarcv.test.DangerousNamesTest","testMethod":"test[param = !!!!!!!]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.355 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000004:{"testClass":"com.github.tarcv.test.DangerousNamesTest","testMethod":"test[param = ''''''']","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.355 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000005:{"testClass":"com.github.tarcv.test.DangerousNamesTest","testMethod":"test[param = \"\"\"\"\"\"\"\"]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.355 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000006:{"testClass":"com.github.tarcv.test.DangerousNamesTest","testMethod":"test[param = ()${'$'}(echo)`echo`()${'$'}(echo)`echo`()${'$'}(echo)`echo`()${'$'}(echo)`echo`()${'$'}(echo)`echo`()${'$'}(echo)`echo`()${'$'}(echo)`echo`]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.356 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000007:{"testClass":"com.github.tarcv.test.DangerousNamesTest","testMethod":"test[param = * *.* * *.* * *.* * *.* * *.* * *.* * *.* * *.* *]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.356 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000008:{"testClass":"com.github.tarcv.test.DangerousNamesTest","testMethod":"test[param = . .. . .. . .. . .. . .. . .. . .. . .. . .. . ..]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.356 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000009:{"testClass":"com.github.tarcv.test.DangerousNamesTest","testMethod":"test[param = |&;<>()${'$'}`?[]#~=%|&;<>()${'$'}`?[]#~=%|&;<>()${'$'}`?[]#~=%|&;<>()${'$'}`?[]#~=%|&;<>()${'$'}`?[]#~=%|&;<>()${'$'}`?[]#~=%|&;<>()${'$'}`?[]#~=%]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.357 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-0000000a:{"testClass":"com.github.tarcv.test.DangerousNamesTest","testMethod":"test[param = ; function {}; while {}; for {}; do {}; done {}; exit]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.357 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-0000000b:{"testClass":"com.github.tarcv.test.GrantPermissionsForClassTest","testMethod":"testPermissionGranted1","annotations":[{"annotationType":"com.github.tarcv.tongs.GrantPermission","value":["android.permission.WRITE_CALENDAR"]},{"annotationType":"com.github.tarcv.tongs.GrantPermission","value":["android.permission.WRITE_CALENDAR"]},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.357 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-0000000c:{"testClass":"com.github.tarcv.test.GrantPermissionsForClassTest","testMethod":"testPermissionGranted2","annotations":[{"annotationType":"com.github.tarcv.tongs.GrantPermission","value":["android.permission.WRITE_CALENDAR"]},{"annotationType":"com.github.tarcv.tongs.GrantPermission","value":["android.permission.WRITE_CALENDAR"]},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.358 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-0000000d:{"testClass":"com.github.tarcv.test.GrantPermissionsForInheritedClassTest","testMethod":"testPermissionGranted1","annotations":[{"annotationType":"com.github.tarcv.tongs.GrantPermission","value":["android.permission.WRITE_CALENDAR"]},{"annotationType":"com.github.tarcv.tongs.GrantPermission","value":["android.permission.WRITE_CALENDAR"]},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.358 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-0000000e:{"testClass":"com.github.tarcv.test.GrantPermissionsForInheritedClassTest","testMethod":"testPermissionGranted2","annotations":[{"annotationType":"com.github.tarcv.tongs.GrantPermission","value":["android.permission.WRITE_CALENDAR"]},{"annotationType":"com.github.tarcv.tongs.GrantPermission","value":["android.permission.WRITE_CALENDAR"]},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.358 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-0000000f:{"testClass":"com.github.tarcv.test.GrantPermissionsTest","testMethod":"testPermissionGranted","annotations":[{"annotationType":"com.github.tarcv.tongs.GrantPermission","value":["android.permission.WRITE_CALENDAR"]},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.359 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000010:{"testClass":"com.github.tarcv.test.GrantPermissionsTest","testMethod":"testNoPermissionByDefault","annotations":[{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.359 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000011:{"testClass":"com.github.tarcv.test.NoPermissionsForOverridesTest","testMethod":"testNoPermissionForOverrides","annotations":[{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.359 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000012:{"testClass":"com.github.tarcv.test.NormalTest","testMethod":"test","annotations":[{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.360 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000013:{"testClass":"com.github.tarcv.test.ParameterizedNamedTest","testMethod":"test[param = 1]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.360 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000014:{"testClass":"com.github.tarcv.test.ParameterizedNamedTest","testMethod":"test[param = 2]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.360 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000015:{"testClass":"com.github.tarcv.test.ParameterizedNamedTest","testMethod":"test[param = 3]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.363 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000016:{"testClass":"com.github.tarcv.test.ParameterizedNamedTest","testMethod":"test[param = 4]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.364 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000017:{"testClass":"com.github.tarcv.test.ParameterizedNamedTest","testMethod":"test[param = 5]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.364 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000018:{"testClass":"com.github.tarcv.test.ParameterizedNamedTest","testMethod":"test[param = 6]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.364 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000019:{"testClass":"com.github.tarcv.test.ParameterizedNamedTest","testMethod":"test[param = 7]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.365 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-0000001a:{"testClass":"com.github.tarcv.test.ParameterizedNamedTest","testMethod":"test[param = 8]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.367 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-0000001b:{"testClass":"com.github.tarcv.test.ParameterizedTest","testMethod":"test[0]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.368 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-0000001c:{"testClass":"com.github.tarcv.test.ParameterizedTest","testMethod":"test[1]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.368 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-0000001d:{"testClass":"com.github.tarcv.test.ParameterizedTest","testMethod":"test[2]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.368 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-0000001e:{"testClass":"com.github.tarcv.test.ParameterizedTest","testMethod":"test[3]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.369 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-0000001f:{"testClass":"com.github.tarcv.test.ParameterizedTest","testMethod":"test[4]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.369 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000020:{"testClass":"com.github.tarcv.test.ParameterizedTest","testMethod":"test[5]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.369 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000021:{"testClass":"com.github.tarcv.test.ParameterizedTest","testMethod":"test[6]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.370 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000022:{"testClass":"com.github.tarcv.test.ParameterizedTest","testMethod":"test[7]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.370 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000023:{"testClass":"com.github.tarcv.test.PropertiesTest","testMethod":"normalPropertiesTest","annotations":[{"annotationType":"com.github.tarcv.tongs.TestProperties","keys":["x","y"],"values":["1","2"]},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.370 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000024:{"testClass":"com.github.tarcv.test.PropertiesTest","testMethod":"normalPropertyPairsTest","annotations":[{"annotationType":"com.github.tarcv.tongs.TestPropertyPairs","value":["v","1","w","2"]},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.372 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000025:{"testClass":"com.github.tarcv.test.ResetPrefsTest","testMethod":"testPrefsAreClearedBetweenTests[0]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.372 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000026:{"testClass":"com.github.tarcv.test.ResetPrefsTest","testMethod":"testPrefsAreClearedBetweenTests[1]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.373 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000027:{"testClass":"com.github.tarcv.test.ResetPrefsTest","testMethod":"testPrefsAreClearedBetweenTests[2]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                "",
                "[ 01-02 00:01:02.373 1234: 5678 I/Tongs.TestInfo ]",
                """0b2d3157-00000028:{"testClass":"com.github.tarcv.test.ResetPrefsTest","testMethod":"testPrefsAreClearedBetweenTests[3]","annotations":[{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.runner.RunWith","value":"class org.junit.runners.Parameterized"},{"annotationType":"org.junit.Test","expected":"class org.junit.Test${'$'}None","timeout":0}]},""",
                ""
        )
    }
}

private class BroadcastingListener(
        private val targetListeners: Collection<ITestRunListener>
) : ITestRunListener {
    override fun testRunStarted(runName: String?, testCount: Int) {
        targetListeners.forEach {
            it.testRunStarted(runName, testCount)
        }
    }

    override fun testStarted(test: TestIdentifier?) {
        targetListeners.forEach {
            it.testStarted(test)
        }
    }

    override fun testAssumptionFailure(test: TestIdentifier?, trace: String?) {
        targetListeners.forEach {
            it.testAssumptionFailure(test, trace)
        }
    }

    override fun testRunStopped(elapsedTime: Long) {
        targetListeners.forEach {
            it.testRunStopped(elapsedTime)
        }
    }

    override fun testFailed(test: TestIdentifier?, trace: String?) {
        targetListeners.forEach {
            it.testFailed(test, trace)
        }
    }

    override fun testEnded(test: TestIdentifier?, testMetrics: Map<String, String>?) {
        targetListeners.forEach {
            it.testEnded(test, testMetrics)
        }
    }

    override fun testIgnored(test: TestIdentifier?) {
        targetListeners.forEach {
            it.testIgnored(test)
        }
    }

    override fun testRunFailed(errorMessage: String?) {
        targetListeners.forEach {
            it.testRunFailed(errorMessage)
        }
    }

    override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>?) {
        targetListeners.forEach {
            it.testRunEnded(elapsedTime, runMetrics)
        }
    }

}