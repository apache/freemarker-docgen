/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.freemarker.docgen.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to install Pagefind and run its indexing process
 * as part of a Java build or site generation.
 */
final class PageFindRunner {
    // See latest version here: https://www.npmjs.com/package/pagefind
    private static final String PAGEFIND_VERSION = "1.3.0";

    private final Path indexedDir;

    /**
     * Constructs a PageFindRunner instance.
     *
     * @param indexedDir The path to the directory containing the fully generated static HTML site
     * that needs to be indexed by Pagefind.
     * @param workDir    The path to a working directory where Node.js (npm) will install Pagefind.
     * This directory will contain `node_modules` and potentially `package.json`.
     */
    private PageFindRunner(Path indexedDir) {
        this.indexedDir = indexedDir.toAbsolutePath().normalize();
    }

    /**
     * Static method to initiate the Pagefind installation and indexing process.
     *
     * @param indexedDir The path to the directory containing the fully generated static HTML site.
     * @param workDir    The path to a working directory for Pagefind installation.
     * @throws RuntimeException If an IOException, InterruptedException, or an error during
     * Pagefind command execution occurs.
     */
    static void run(Path indexedDir) {
        try {
            new PageFindRunner(indexedDir).run();
        } catch (IOException | InterruptedException e) {
            // Wrap checked exceptions in a RuntimeException for simpler API usage
            throw new RuntimeException("Pagefind execution failed (see cause exception)", e);
        }
    }

    /**
     * Executes the Pagefind installation and indexing.
     */
    private void run() throws IOException, InterruptedException {
        if (!Files.isDirectory(indexedDir)) {
            throw new IllegalArgumentException("Indexed directory must be a valid directory: " + indexedDir);
        }

        System.out.println("Invoking Pagefind on indexed directory: " + indexedDir);

        int exitCode = runNodeProcess();
        if (exitCode != 0) {
            throw new RuntimeException("Pagefind indexing command failed with exit code: " + exitCode);
        }

        System.out.println("Pagefind indexing completed successfully.");
    }

    private int runNodeProcess() throws IOException, InterruptedException {
        String npxCommand = findNpxCommand();

        ProcessBuilder processBuilder = new ProcessBuilder(
                npxCommand,
                "pagefind@" + PAGEFIND_VERSION,
                "--site",
                indexedDir.toString()
        );

        // Just to have a consistent work directory...
        processBuilder.directory(indexedDir.toFile());

        // Redirect standard output and standard error to the Java application's console
        processBuilder.inheritIO();

        Process process = processBuilder.start();

        return process.waitFor();
    }

    private String findNpxCommand() throws IOException, InterruptedException {
        Map<String, Exception> failedAttempts = new LinkedHashMap<>();
        for (String npxCommand : List.of("npx", "npx.cmd", "npx.ps1")) {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    npxCommand, "--version"
            );

            // Just to have a consistent work directory...
            processBuilder.directory(indexedDir.toFile());
            try {
                Process process = processBuilder.start();
                String output = new String(process.getInputStream().readAllBytes());
                process.waitFor();
                if (process.exitValue() != 0) {
                    throw new IllegalStateException(npxCommand + " --version command failed with exit code " + process.exitValue());
                }
                System.out.println(npxCommand + " --version output:\n" + output);
                return npxCommand;
            } catch (IOException e) {
                failedAttempts.put(npxCommand, e);
            }
        }

        StringBuilder sb = new StringBuilder("Can't find a working npx command. "
                + "Ensure that Node.js is installed, and it's in the system path!\n");
        for (Map.Entry<String, Exception> entry : failedAttempts.entrySet()) {
            StringWriter stackTrace = new StringWriter();
            sb.append("\nAttempted command: ").append(entry.getKey()).append("\n")
                    .append("Failed with: ").append(ExceptionUtils.toCauseTrace(entry.getValue()))
                    .append("\n");
        }
        throw new IOException(sb.toString());
    }

    // For testing purposes only!
    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Command-line arguments should be: <indexedDir>");
        }
        PageFindRunner.run(Path.of(args[0]));
    }
}
