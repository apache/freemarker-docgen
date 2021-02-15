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

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class BashCommandLineArgsParserTest {

    @Test
    void test1() {
        assertEquals(Arrays.asList(), BashCommandLineArgsParser.parse(""));
        assertEquals(Arrays.asList(), BashCommandLineArgsParser.parse( " "));
        assertEquals(Arrays.asList("cmd", "1", "2", "3"), BashCommandLineArgsParser.parse("cmd 1\t2\r\n3"));
        assertEquals(Arrays.asList("1 x", "2 x", "a'bcd"), BashCommandLineArgsParser.parse("'1 x' \"2 x\" a\"'\"b'c'd"));
        assertEquals(Arrays.asList("abc"), BashCommandLineArgsParser.parse("a\\bc"));
        assertEquals(Arrays.asList("a\\bc"), BashCommandLineArgsParser.parse("a\\\\bc"));
        assertEquals(Arrays.asList("a'bc", "d  e"), BashCommandLineArgsParser.parse("a\\'bc d\\ \\ e"));
        assertEquals(Arrays.asList("a\\b\\c\"$"), BashCommandLineArgsParser.parse("\"a\\b\\\\c\\\"\\$\""));
        assertEquals(Arrays.asList("a\\b\\\\c"), BashCommandLineArgsParser.parse("'a\\b\\\\c'"));
    }

    @Test
    void testBackslashLineBreak() {
        assertEquals(Arrays.asList("ab", "c"), BashCommandLineArgsParser.parse("a\\\nb\\\n c"));
        assertEquals(Arrays.asList("ab", "c"), BashCommandLineArgsParser.parse("a\\\r\nb\\\r\n c"));
        assertEquals(Arrays.asList("a ab", "c"), BashCommandLineArgsParser.parse("a\\ a\\\r\nb \\\r\n c"));
    }

}