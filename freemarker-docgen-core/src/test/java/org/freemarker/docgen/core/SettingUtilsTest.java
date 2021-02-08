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

import static org.freemarker.docgen.core.SettingUtils.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class SettingUtilsTest {

    private static final SettingName SETTING_NAME = SettingName.topLevel(null, "foo");

    @Test
    public void testBasics() {
        {
            Object originalValue = 1;
            Object value = castSetting(SettingName.topLevel(null, "a"), originalValue, Integer.class);
            assertEquals(originalValue, value);
        }
        {
            ImmutableList<ImmutableList<?>> originalValue = ImmutableList.of(ImmutableList.of(), ImmutableList.of(1));
            Object value = castSetting(
                    SETTING_NAME,
                    originalValue, DefaultValue.NULL,
                    List.class,
                    new ListItemType(List.class),
                    new ListItemType(Integer.class)
            );
            assertEquals(originalValue, value);
        }
        {
            ImmutableMap<String, ImmutableMap<?, ?>> originalValue = ImmutableMap.of(
                    "x", ImmutableMap.of(),
                    "y", ImmutableMap.of("u", 1));
            Object value = castSetting(
                    SETTING_NAME,
                    originalValue,
                    Map.class,
                    new MapEntryType<>(String.class, Map.class),
                    new MapEntryType(String.class, Integer.class)
            );
            assertEquals(originalValue, value);
        }
    }

    @Test
    public void testOptional() {
        assertNull(castSetting(SETTING_NAME, null, DefaultValue.NULL, Integer.class));
        try {
            castSetting(SETTING_NAME, null, Integer.class);
            fail();
        } catch (DocgenException e) {
            assertThat(
                    e.getMessage(),
                    allOf(containsString("required"), containsString(SETTING_NAME.toString())));
        }
    }

    @Test
    public void testMapKeyValidation() {
        ImmutableSet<String> requiredKeys = ImmutableSet.of("reqKey1", "reqKey2");
        ImmutableSet<String> optionalKeys = ImmutableSet.of("optKey");
        {
            ImmutableMap<String, Integer> originalValue = ImmutableMap.of(
                    "reqKey1", 1,
                    "reqKey2", 2,
                    "optKey", 3);
            Object value = castSetting(
                    SETTING_NAME, originalValue,
                    Map.class,
                    new MapEntryType(
                            String.class, requiredKeys, optionalKeys,
                            Integer.class));
            assertEquals(originalValue, value);
        }
        {
            ImmutableMap<String, Integer> originalValue = ImmutableMap.of(
                    "reqKey1", 1,
                    "reqKey2", 2);
            Object value = castSetting(
                    SETTING_NAME,
                    originalValue,
                    Map.class,
                    new MapEntryType(
                            String.class, requiredKeys, optionalKeys,
                            Integer.class));
            assertEquals(originalValue, value);
        }
        try {
            castSetting(
                    SETTING_NAME,
                    ImmutableMap.of(
                            "reqKey1", 1,
                            "optKey", 3),
                    Map.class,
                    new MapEntryType(
                            String.class, requiredKeys, optionalKeys,
                            Integer.class));
            fail();
        } catch (DocgenException e) {
            assertThat(
                    e.getMessage(),
                    allOf(containsString("reqKey2"), containsString(SETTING_NAME.toString())));
        }
        try {
            castSetting(
                    SETTING_NAME,
                    ImmutableMap.of(
                            "reqKey1", 1,
                            "reqKey2", 2,
                            "wrongKey", 2),
                    Map.class,
                    new MapEntryType(
                            String.class, requiredKeys, optionalKeys,
                            Integer.class));
            fail();
        } catch (DocgenException e) {
            assertThat(
                    e.getMessage(),
                    allOf(containsString("wrongKey"), containsString(SETTING_NAME.toString())));
        }
    }

}
