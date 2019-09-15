package com.github.tarcv.tongs.device

import com.android.ddmlib.*
import com.github.tarcv.tongs.model.Diagnostics
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

fun computeDiagnostics(deviceInterface: IDevice?, apiLevel: Int): Diagnostics {
    if (deviceInterface == null) {
        return Diagnostics.NONE
    }

    val supportsScreenRecord = deviceInterface.supportsFeature(IDevice.Feature.SCREEN_RECORD) &&
            "Genymotion" != deviceInterface.getProperty("ro.product.manufacturer")
    if (supportsScreenRecord) {
        return Diagnostics.VIDEO
    }

    return if (apiLevel >= 16) {
        Diagnostics.SCREENSHOTS
    } else Diagnostics.NONE

}

private val logger = LoggerFactory.getLogger(" com.github.tarcv.tongs.device.DeviceUtils")