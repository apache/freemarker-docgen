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

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a bash command call to a list of arguments. Quotation and escaping is resolved in the returned arguments.
 */
public class BashCommandLineArgsParser {
    private final String src;
    private int pos;

    public BashCommandLineArgsParser(String src) {
        this.src = src;
    }

    public static List<String> parse(String s) {
        return new BashCommandLineArgsParser(s).parse();
    }

    private List<String> parse() {
        List<String> args = new ArrayList<>();
        String arg;
        while ((arg = skipWSAndFetchArg()) != null) {
            args.add(arg);
        }
        return args;
    }

    private String skipWSAndFetchArg() {
        skipWS();
        return fetchArg();
    }

    private String fetchArg() {
        StringBuilder arg = new StringBuilder();
        int startPos = pos;
        char openedQuote = 0;
        boolean escaped = false;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (escaped) {
                if (openedQuote == '"' && !(c == '"' || c == '\\' || c == '$')) {
                    arg.append('\\');
                }
                arg.append(c);
                escaped = false;
            } else {
                if (c == '"' || c == '\'') {
                    if (openedQuote == 0) {
                        openedQuote = c;
                    } else if (openedQuote == c) {
                        openedQuote = 0;
                    } else {
                        arg.append(c);
                    }
                } else if (c == '\\' && openedQuote != '\'') {
                    escaped = true;
                } else if (openedQuote == 0 && isWS(c)) {
                    break;
                } else {
                    arg.append(c);
                }
            }
            pos++;
        }
        return startPos != pos ? arg.toString() : null;
    }

    private void skipWS() {
        while (pos < src.length() && isWS(src.charAt(pos))) {
            pos++;
        }
    }

    private boolean isWS(char c) {
        return c == ' ' || c == '\n' || c == '\r' || c == '\t';
    }
}
