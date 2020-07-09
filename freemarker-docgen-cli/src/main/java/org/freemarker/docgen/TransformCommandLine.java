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
package org.freemarker.docgen;

import java.io.File;
import java.io.IOException;
import java.util.TimeZone;

import org.xml.sax.SAXException;

/**
 * Command-line "interface" to {@link Transform}.
 */
public final class TransformCommandLine {

    // Can't be instantiated
    private TransformCommandLine() {
        // Nop
    }

    public static void main(String[] args)
            throws DocgenException, IOException, SAXException {
        try {
            if (args.length < 2) {
                throw new CommandLineExitException(-1,
                        "Usage: java -jar docgen.jar <srcDir> <dstDir> [option1=value1 ...]");
            }

            Transform tr = new Transform();
            tr.setSourceDirectory(new File(args[0]));
            tr.setDestinationDirectory(new File(args[1]));
            tr.setPrintProgress(true);

            for (int i = 2; i < args.length; i++) {
                String optStr = args[i];
                final int equalsIdx = optStr.indexOf('=');
                if (equalsIdx == -1) {
                    throw new CommandLineExitException(-1, "Options must be in name=value format");
                }
                String name = optStr.substring(0, equalsIdx).trim();
                String value = optStr.substring(equalsIdx + 1).trim();

                if (name.equals("offline")) {
                    tr.setOffline(parseBoolean(value));
                } else if (name.equals("timeZone")) {
                    tr.setTimeZone(TimeZone.getTimeZone(value));
                } else if (name.equals("generateEclipseToC")) {
                    tr.setGenerateEclipseToC(parseBoolean(value));
                } else {
                    throw new CommandLineExitException(-1, "Unsupported option: " + name);
                }
            }

            tr.execute();
        } catch (CommandLineExitException e) {
            p(e.getMessage().replaceAll("\n", System.lineSeparator()));
            System.exit(e.getExitCode());
        }
    }

    private static boolean parseBoolean(String value) throws CommandLineExitException {
        if (value.equals("true")) return true;
        if (value.equals("false")) return false;
        throw new CommandLineExitException(-1, "Malformed boolean: " + value);
    }

    static void p(Object o) {
        System.out.println(o);
    }

}
