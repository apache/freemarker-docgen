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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.Test;

public class ChopLinebreakWriterTest {

    @Test
    public void testCharArg1() throws IOException {
        StringWriter sw = new StringWriter();
        Writer w = new ChopLinebreakWriter(sw);

        w.write('a');
        w.write('\n');
        w.write('b');
        w.write('\n');
        assertEquals("a\nb", sw.toString());
    }

    @Test
    public void testStringArg1() throws IOException {
        StringWriter sw = new StringWriter();
        Writer w = new ChopLinebreakWriter(sw);

        w.write("a");
        assertEquals("a", sw.toString());
        w.write("b");
        assertEquals("ab", sw.toString());
        w.write("\r");
        assertEquals("ab", sw.toString());
        w.write("\n");
        assertEquals("ab", sw.toString());
        w.write("c");
        assertEquals("ab\r\nc", sw.toString());
        w.write("\ndef\r");
        assertEquals("ab\r\nc\ndef", sw.toString());
        w.write("g\n");
        assertEquals("ab\r\nc\ndef\rg", sw.toString());
        w.write("\nh");
        assertEquals("ab\r\nc\ndef\rg\n\nh", sw.toString());
    }

    @Test
    public void testStrignArg3() throws IOException {
        StringWriter sw = new StringWriter();
        Writer w = new ChopLinebreakWriter(sw);

        w.write("\n\n\n\r");
        assertEquals("\n\n\n", sw.toString());
        w.write("\n");
        assertEquals("\n\n\n", sw.toString());
        w.write("\r");
        assertEquals("\n\n\n\r\n", sw.toString());
        w.write("\n");
        assertEquals("\n\n\n\r\n", sw.toString());
        w.write("c");
        assertEquals("\n\n\n\r\n\r\nc", sw.toString());
    }

    @Test
    public void testStringArg2() throws IOException {
        StringWriter sw = new StringWriter();
        Writer w = new ChopLinebreakWriter(sw);

        w.write("a\n\n");
        assertEquals("a\n", sw.toString());
        w.write("b");
        assertEquals("a\n\nb", sw.toString());
    }

    @Test
    public void testCharArray() throws IOException {
        StringWriter sw = new StringWriter();
        Writer w = new ChopLinebreakWriter(sw);

        w.write("a".toCharArray());
        assertEquals("a", sw.toString());
        w.write("\nb\n".toCharArray());
        assertEquals("a\nb", sw.toString());
        w.write("\nc".toCharArray());
        assertEquals("a\nb\n\nc", sw.toString());
        w.write("def\n\nghi\n\n".toCharArray());
        assertEquals("a\nb\n\ncdef\n\nghi\n", sw.toString());
    }

    @Test
    public void testSubstringWrites() throws IOException {
        StringWriter sw = new StringWriter();
        Writer w = new ChopLinebreakWriter(sw);

        String s = "0123\n567\n\nA";
        w.write(s, 1, 3);
        assertEquals("123", sw.toString());
        w.write(s, 4, 1);
        assertEquals("123", sw.toString());
        w.write(s, 5, 2);
        assertEquals("123\n56", sw.toString());
        w.write(s, 7, 3);
        assertEquals("123\n567\n", sw.toString());
        w.write(s, 10, 1);
        assertEquals("123\n567\n\nA", sw.toString());
    }

    @Test
    public void testSubArrayWrites() throws IOException {
        StringWriter sw = new StringWriter();
        Writer w = new ChopLinebreakWriter(sw);

        String s = "0123\n567\n\nA";
        w.write(s.toCharArray(), 1, 3);
        assertEquals("123", sw.toString());
        w.write(s.toCharArray(), 4, 1);
        assertEquals("123", sw.toString());
        w.write(s.toCharArray(), 5, 2);
        assertEquals("123\n56", sw.toString());
        w.write(s.toCharArray(), 7, 3);
        assertEquals("123\n567\n", sw.toString());
        w.write(s.toCharArray(), 10, 1);
        assertEquals("123\n567\n\nA", sw.toString());
    }

}