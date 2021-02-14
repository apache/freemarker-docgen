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

class EscapeHtmlAndXmlWriterTest {

    @Test
    public void testCharArg1() throws IOException {
        StringWriter sw = new StringWriter();
        Writer w = new EscapeHtmlAndXmlWriter(sw);

        w.write('a');
        w.write('<');
        w.write('>');
        w.write('b');
        w.write('&');
        assertEquals("a&lt;&gt;b&amp;", sw.toString());
    }

    @Test
    public void testStringArg1() throws IOException {
        StringWriter sw = new StringWriter();
        Writer w = new EscapeHtmlAndXmlWriter(sw);

        w.write("");
        w.write("a");
        w.write("b<cd>ef&g");
        assertEquals("ab&lt;cd&gt;ef&amp;g", sw.toString());
        w.write("<>");
        assertEquals("ab&lt;cd&gt;ef&amp;g&lt;&gt;", sw.toString());
        w.write("<");
        assertEquals("ab&lt;cd&gt;ef&amp;g&lt;&gt;&lt;", sw.toString());
    }

    @Test
    public void testArrayArg1() throws IOException {
        StringWriter sw = new StringWriter();
        Writer w = new EscapeHtmlAndXmlWriter(sw);

        w.write("".toCharArray());
        w.write("a".toCharArray());
        w.write("b<cd>ef&g".toCharArray());
        assertEquals("ab&lt;cd&gt;ef&amp;g", sw.toString());
        w.write("<>".toCharArray());
        assertEquals("ab&lt;cd&gt;ef&amp;g&lt;&gt;", sw.toString());
        w.write("<".toCharArray());
        assertEquals("ab&lt;cd&gt;ef&amp;g&lt;&gt;&lt;", sw.toString());
    }

}