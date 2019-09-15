package com.github.tarcv.tongs.runner

import com.android.ddmlib.*
import com.github.tarcv.tongs.model.AndroidDevice
import com.github.tarcv.tongs.model.Device
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.String.format

class AndroidCleanupTestRuleFactory : TestRuleFactory<AndroidDevice, AndroidCleanupTestRule> {
    override fun create(context: TongsTestCaseContext<AndroidDevice>): AndroidCleanupTestRule {
        return AndroidCleanupTestRule(
                context.device,
                context.configuration.applicationPackage,
                context.configuration.instrumentationPackage
        )
    }
}

class AndroidCleanupTestRule(
        device: AndroidDevice,
        private val applicationPackage: String,
        private val testPackage: String
) : TestRule<AndroidDevice> {
    private val logger = LoggerFactory.getLogger(AndroidInstrumentedTestRun::class.java)
    private val device: IDevice = device.deviceInterface

    override fun before() {
        clearPackageData(device, applicationPackage)
        clearPackageData(device, testPackage)
        resetToHomeScreen()
    }

    override fun after() {
    }

    /**
     * Reset device to Home Screen and close soft keyboard if it is still open
     */
    private fun resetToHomeScreen() {
        val noOpReceiver = NullOutputReceiver()
        device.executeShellCommand("input keyevent 3", noOpReceiver) // HOME
        device.executeShellCommand("input keyevent 4", noOpReceiver) // BACK
    }

    private fun clearPackageData(device: IDevice, applicationPackage: String) {
        val start = System.currentTimeMillis()

        try {
            val command = format("pm clear %s", applicationPackage)
            logger.info("Cmd: $command")
            device.executeShellCommand(command, NullOutputReceiver())
        } catch (e: TimeoutException) {
            throw UnsupportedOperationException(format("Unable to clear package data (%s)", applicationPackage), e)
        } catch (e: AdbCommandRejectedException) {
            throw UnsupportedOperationException(format("Unable to clear package data (%s)", applicationPackage), e)
        } catch (e: ShellCommandUnresponsiveException) {
            throw UnsupportedOperationException(format("Unable to clear package data (%s)", applicationPackage), e)
        } catch (e: IOException) {
            throw UnsupportedOperationException(format("Unable to clear package data (%s)", applicationPackage), e)
        }

        logger.debug("Clearing application data: {} (took {}ms)", applicationPackage, System.currentTimeMillis() - start)
    }

}