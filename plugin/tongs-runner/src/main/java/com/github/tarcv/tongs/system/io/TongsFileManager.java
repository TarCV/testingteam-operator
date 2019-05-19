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

package com.github.tarcv.tongs.system.io;

import com.android.ddmlib.testrunner.TestIdentifier;
import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.model.TestCaseEvent;

import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.tarcv.tongs.CommonDefaults.TONGS_SUMMARY_FILENAME_FORMAT;
import static com.github.tarcv.tongs.system.io.FileType.TEST;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Paths.get;

public class TongsFileManager implements FileManager {
    private final File output;

    public TongsFileManager(File output) {
        this.output = output;
    }

    @Override
    public File[] getTestFilesForDevice(Pool pool, Device serial) {
        Path path = getDirectory(TEST, pool, serial);
        return path.toFile().listFiles();
    }

    @Override
    public File createFile(FileType fileType, Pool pool, Device device, TestCaseEvent testCaseEvent) {
        return createFile(fileType, pool, device, new TestIdentifier(testCaseEvent.getTestClass(), testCaseEvent.getTestMethod()));
    }

    @Override
    public File createFile(FileType fileType, Pool pool, Device device, TestIdentifier testIdentifier, int sequenceNumber) {
        try {
            Path directory = createDirectory(fileType, pool, device);
            String filename = createFilenameForTest(testIdentifier, fileType, sequenceNumber);
            return createFile(directory, filename);
        } catch (IOException e) {
            throw new CouldNotCreateDirectoryException(e);
        }
    }

    @Override
    public File createFile(FileType fileType, Pool pool, Device device, TestIdentifier testIdentifier) {
        try {
            Path directory = createDirectory(fileType, pool, device);
            String filename = createFilenameForTest(testIdentifier, fileType);
            return createFile(directory, filename);
        } catch (IOException e) {
            throw new CouldNotCreateDirectoryException(e);
        }
    }

    @Override
    public File createSummaryFile() {
        try {
            Path path = get(output.getAbsolutePath(), "summary");
            Path directory = createDirectories(path);
            return createFile(directory, String.format(TONGS_SUMMARY_FILENAME_FORMAT, System.currentTimeMillis()));
        } catch (IOException e) {
            throw new CouldNotCreateDirectoryException(e);
        }
    }

    @Override
    public File[] getFiles(FileType fileType, Pool pool, Device device, TestIdentifier testIdentifier) {
        FileFilter fileFilter = new AndFileFilter(
                new PrefixFileFilter(testIdentifier.toString()),
                new SuffixFileFilter(fileType.getSuffix()));

        File deviceDirectory = get(output.getAbsolutePath(), fileType.getDirectory(), pool.getName(), device.getSafeSerial()).toFile();
        return deviceDirectory.listFiles(fileFilter);
    }

    @Override
    public File getFile(FileType fileType, String pool, String safeSerial, TestIdentifier testIdentifier) {
        String filenameForTest = createFilenameForTest(testIdentifier, fileType);
        Path path = get(output.getAbsolutePath(), fileType.getDirectory(), pool, safeSerial, filenameForTest);
        return path.toFile();
    }

    private Path createDirectory(FileType test, Pool pool, Device device) throws IOException {
        return createDirectories(getDirectory(test, pool, device));
    }

    private Path getDirectory(FileType fileType, Pool pool, Device device) {
        return get(output.getAbsolutePath(), fileType.getDirectory(), pool.getName(), device.getSafeSerial());
    }

    private File createFile(Path directory, String filename) {
        return new File(directory.toFile(), filename);
    }

    @Override
    public String createFilenameForTest(TestIdentifier testIdentifier, FileType fileType) {
        String testName = testIdentifier.toString();

        // Test identifier can contain absolutely any characters, so generate safe name out of it
        // Dots are not safe because of '.', '..' and extensions
        String safeChars = testName.replaceAll("[^A-Za-z0-9_]", "_");

        // Always use hash to handle edge case of test name that differ only with char case
        String hash;
        try {
            byte[] hashBytes = MessageDigest.getInstance("MD5")
                    .digest(testName.getBytes(StandardCharsets.UTF_8));
            hash = IntStream.range(0, hashBytes.length)
                    .map(i -> {
                        if (hashBytes[i] >= 0) {
                            return hashBytes[i];
                        } else {
                            return 0x100 + hashBytes[i];
                        }
                    })
                    .skip(hashBytes.length / 2) // avoid too long file names
                    .mapToObj(b -> String.format("%02x", b))
                    .collect(Collectors.joining());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate safe file name");
        }

        return String.format("%s-%s.%s", safeChars, hash, fileType.getSuffix());
    }

    private String createFilenameForTest(TestIdentifier testIdentifier, FileType fileType, int sequenceNumber) {
        return String.format("%s-%02d.%s", testIdentifier.toString(), sequenceNumber, fileType.getSuffix());
    }
}