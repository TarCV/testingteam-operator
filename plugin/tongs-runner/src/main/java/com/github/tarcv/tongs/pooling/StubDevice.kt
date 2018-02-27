/*
 * Copyright 2018 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.pooling

import com.android.ddmlib.*
import com.android.ddmlib.IDevice.Feature.SCREEN_RECORD
import com.android.ddmlib.log.LogReceiver
import com.android.sdklib.AndroidVersion
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.lang.Thread.sleep
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class StubDevice(
        private val serial: String,
        private val manufacturer: String,
        private val model: String,
        private val name: String,
        private val api: Int,
        private val characteristics: String,
        private val testCommandDelay: Long
) : IDevice {
    val deviceLogFile = File("${serial}_adb.log")

    override fun startScreenRecorder(remoteFilePath: String, options: ScreenRecorderOptions, receiver: IShellOutputReceiver) {
        synchronized(this) {
            val optionsStr = "{dimen=${options.width}x${options.height}" +
                    ", Mbps=${options.bitrateMbps}" +
                    ", limit=${options.timeLimit} ${options.timeLimitUnits}}"
            FileWriter(deviceLogFile, true).apply {
                write("${System.currentTimeMillis()}\t[START SCREEN RECORDER]" +
                        " $remoteFilePath,$optionsStr${System.lineSeparator()}")
                close()
            }
        }
    }

    override fun getName(): String = name

    override fun executeShellCommand(command: String, receiver: IShellOutputReceiver) {
        synchronized(this) {
            val outputBytes = "<stub> <stub> <stub> <stub> <stub>".toByteArray()

            if (command.contains("am instrument")) {
                sleep(testCommandDelay)
            }

            FileWriter(deviceLogFile, true).apply {
                write("${System.currentTimeMillis()}\t$command${System.lineSeparator()}")
                close()
            }

            receiver.addOutput(outputBytes, 0, outputBytes.size)
            receiver.flush()
        }
    }

    override fun executeShellCommand(command: String, receiver: IShellOutputReceiver, maxTimeToOutputResponse: Int) {
        executeShellCommand(command, receiver)
    }

    override fun executeShellCommand(command: String, receiver: IShellOutputReceiver, maxTimeToOutputResponse: Long, maxTimeUnits: TimeUnit?) {
        executeShellCommand(command, receiver)
    }

    override fun getProperty(name: String): String {
        return when (name) {
            "ro.product.manufacturer" -> manufacturer
            "ro.product.model" -> model
            "ro.build.version.sdk" -> api.toString()
            "ro.build.characteristics" -> characteristics
            else -> ""
        }
    }

    @Throws(IOException::class)
    override fun pullFile(remote: String, local: String) {
        synchronized(this) {
            FileWriter(deviceLogFile, true).apply {
                write("${System.currentTimeMillis()}\t[PULL FILE] $remote,$local" +
                        System.lineSeparator())
                close()
            }
        }

        val writer = FileWriter(File(local))
        writer.flush()
        writer.close()
    }

    override fun uninstallPackage(packageName: String): String? {
        synchronized(this) {
            FileWriter(deviceLogFile, true).apply {
                write("${System.currentTimeMillis()}\t[UNINSTALL PACKAGE] $packageName" +
                        System.lineSeparator())
                close()
            }
        }

        return null
    }

    override fun installPackage(
            packageFilePath: String,
            reinstall: Boolean,
            vararg extraArgs: String
    ) {
        synchronized(this) {
            FileWriter(deviceLogFile, true).apply {
                write("${System.currentTimeMillis()}\t[INSTALL PACKAGE] $packageFilePath" +
                        " reinstall=$reinstall extraArgs={${extraArgs.joinToString(" ")}}" +
                        System.lineSeparator())
                close()
            }
        }
    }

    override fun supportsFeature(feature: IDevice.Feature): Boolean {
        return when(feature) {
            SCREEN_RECORD -> true
            else -> false
        }
    }

    override fun getScreenshot(): RawImage {
        TODO("not implemented")
    }

    override fun getVersion(): AndroidVersion = AndroidVersion(api, null)

    override fun getSerialNumber(): String = serial

    override fun isOnline(): Boolean = true


    // Methods below this line are not used Tongs and therefore doesn't need to be stubbed

    override fun isOffline(): Boolean {
        TODO("not implemented")
    }

    override fun reboot(into: String?) {
        TODO("not implemented")
    }

    override fun getMountPoint(name: String?): String {
        TODO("not implemented")
    }

    override fun getClients(): Array<Client> {
        TODO("not implemented")
    }

    override fun runLogService(logname: String?, receiver: LogReceiver?) {
        TODO("not implemented")
    }

    override fun installRemotePackage(remoteFilePath: String?, reinstall: Boolean, vararg extraArgs: String?) {
        TODO("not implemented")
    }

    override fun getClientName(pid: Int): String {
        TODO("not implemented")
    }

    override fun runEventLogService(receiver: LogReceiver?) {
        TODO("not implemented")
    }

    override fun getLanguage(): String {
        TODO("not implemented")
    }

    override fun root(): Boolean {
        TODO("not implemented")
    }

    override fun getSystemProperty(name: String?): Future<String> {
        TODO("not implemented")
    }

    override fun isBootLoader(): Boolean {
        TODO("not implemented")
    }

    override fun isEmulator(): Boolean {
        TODO("not implemented")
    }

    override fun getFileListingService(): FileListingService {
        TODO("not implemented")
    }

    override fun isRoot(): Boolean {
        TODO("not implemented")
    }

    override fun removeForward(localPort: Int, remotePort: Int) {
        TODO("not implemented")
    }

    override fun removeForward(localPort: Int, remoteSocketName: String?, namespace: IDevice.DeviceUnixSocketNamespace?) {
        TODO("not implemented")
    }

    override fun createForward(localPort: Int, remotePort: Int) {
        TODO("not implemented")
    }

    override fun createForward(localPort: Int, remoteSocketName: String?, namespace: IDevice.DeviceUnixSocketNamespace?) {
        TODO("not implemented")
    }

    override fun getAbis(): MutableList<String> {
        TODO("not implemented")
    }

    override fun pushFile(local: String?, remote: String?) {
        TODO("not implemented")
    }

    override fun removeRemotePackage(remoteFilePath: String?) {
        TODO("not implemented")
    }

    override fun getClient(applicationName: String?): Client {
        TODO("not implemented")
    }

    override fun getBattery(): Future<Int> {
        TODO("not implemented")
    }

    override fun getBattery(freshnessTime: Long, timeUnit: TimeUnit?): Future<Int> {
        TODO("not implemented")
    }

    override fun hasClients(): Boolean {
        TODO("not implemented")
    }

    override fun getPropertySync(name: String?): String {
        TODO("not implemented")
    }

    override fun getProperties(): MutableMap<String, String> {
        TODO("not implemented")
    }

    override fun getAvdName(): String {
        TODO("not implemented")
    }

    override fun getRegion(): String {
        TODO("not implemented")
    }

    override fun getState(): IDevice.DeviceState {
        TODO("not implemented")
    }

    override fun getPropertyCacheOrSync(name: String?): String {
        TODO("not implemented")
    }

    override fun installPackages(apks: MutableList<File>?, reinstall: Boolean, installOptions: MutableList<String>?, timeout: Long, timeoutUnit: TimeUnit?) {
        TODO("not implemented")
    }

    override fun getDensity(): Int {
        TODO("not implemented")
    }

    override fun getSyncService(): SyncService {
        TODO("not implemented")
    }

    override fun syncPackageToDevice(localFilePath: String?): String {
        TODO("not implemented")
    }

    override fun arePropertiesSet(): Boolean {
        TODO("not implemented")
    }

    override fun supportsFeature(feature: IDevice.HardwareFeature?): Boolean {
        TODO("not implemented")
    }

    override fun getScreenshot(timeout: Long, unit: TimeUnit?): RawImage {
        TODO("not implemented")
    }

    override fun getBatteryLevel(): Int {
        TODO("not implemented")
    }

    override fun getBatteryLevel(freshnessMs: Long): Int {
        TODO("not implemented")
    }

    override fun getPropertyCount(): Int {
        TODO("not implemented")
    }
}