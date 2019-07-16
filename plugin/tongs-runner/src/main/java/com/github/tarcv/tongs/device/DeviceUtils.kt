package com.github.tarcv.tongs.device

import com.android.ddmlib.*
import org.slf4j.LoggerFactory
import java.io.IOException

fun clearLogcat(device: IDevice) {
    try {
        device.executeShellCommand("logcat -c", NullOutputReceiver())
    } catch (e: TimeoutException) {
        logger.warn("Could not clear logcat on device: " + device.serialNumber, e)
    } catch (e: AdbCommandRejectedException) {
        logger.warn("Could not clear logcat on device: " + device.serialNumber, e)
    } catch (e: ShellCommandUnresponsiveException) {
        logger.warn("Could not clear logcat on device: " + device.serialNumber, e)
    } catch (e: IOException) {
        logger.warn("Could not clear logcat on device: " + device.serialNumber, e)
    }
}

private val logger = LoggerFactory.getLogger(" com.github.tarcv.tongs.device.DeviceUtils")