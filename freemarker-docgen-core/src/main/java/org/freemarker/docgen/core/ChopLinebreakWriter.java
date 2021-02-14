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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Writer that passes content through to a wrapped {@link Writer}, but will remove the line-break from the end of what
 * was written so far.
 */
class ChopLinebreakWriter extends FilterWriter {
    private String pendingLinebreak;

    protected ChopLinebreakWriter(Writer out) {
        super(out);
    }

    @Override
    public void write(int c) throws IOException {
        if (isLinebreakChar(c)) {
            if (pendingLinebreak == null) {
                pendingLinebreak = Character.toString((char) c);
            } else {
                if (c == '\n' && pendingLinebreak.equals("\r")) {
                    pendingLinebreak = "\r\n";
                } else {
                    commitPendingLinebreak();
                    pendingLinebreak = Character.toString((char) c);
                }
            }
        } else {
            commitPendingLinebreak();
            out.write(c);
        }
    }

    private static boolean isLinebreakChar(int c) {
        return c == '\n' || c == '\r';
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        int endOff = off + len;
        int lastNonBrIndex = endOff - 1;
        while (lastNonBrIndex >= off && isLinebreakChar(cbuf[lastNonBrIndex])) {
            lastNonBrIndex--;
        }
        if (lastNonBrIndex >= off) {
            commitPendingLinebreak();
            out.write(cbuf, off, lastNonBrIndex + 1 - off);
        }
        for (int i = lastNonBrIndex + 1; i < endOff; i++) {
            write(cbuf[i]);
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        int endOff = off + len;
        int lastNonBrIndex = endOff - 1;
        while (lastNonBrIndex >= off && isLinebreakChar(str.charAt(lastNonBrIndex))) {
            lastNonBrIndex--;
        }
        if (lastNonBrIndex >= off) {
            commitPendingLinebreak();
            out.write(str, off, lastNonBrIndex + 1 - off);
        }
        for (int i = lastNonBrIndex + 1; i < endOff; i++) {
            write(str.charAt(i));
        }
    }

    private void commitPendingLinebreak() throws IOException {
        if (pendingLinebreak != null) {
            out.write(pendingLinebreak);
            pendingLinebreak = null;
        }
    }

}
