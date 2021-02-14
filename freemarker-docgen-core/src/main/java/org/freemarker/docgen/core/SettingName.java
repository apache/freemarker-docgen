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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import freemarker.ext.beans.NumberModel;
import freemarker.template.utility.StringUtil;

final class SettingName {
    private final File parentFile;
    private final SettingName parent;
    private final Object key;

    public SettingName(File parentFile, SettingName parent, Object key) {
        this.parentFile = parentFile;
        this.parent = parent;
        this.key = Objects.requireNonNull(key);
        if (!(key instanceof String) && !(key instanceof Number)) {
            throw new IllegalArgumentException(
                    "Key must be String or Number, but it was: " + key.getClass().getName());
        }
    }

    static SettingName topLevel(File parentFile, String simpleName) {
        return new SettingName(parentFile, null, simpleName);
    }

    SettingName subKey(Object key) {
        return new SettingName(null, this, key);
    }

    SettingName subKey(Object... keys) {
        return subKey(Arrays.asList(keys));
    }

    SettingName subKey(List<Object> keys) {
        SettingName result = this;
        for (Object key : keys) {
            result = new SettingName(null, result, key);
        }
        return result;
    }

    Optional<File> getContainingFile() {
        return parent != null ? parent.getContainingFile() : Optional.ofNullable(parentFile);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendName(sb);
        return sb.toString();
    }

    private void appendName(StringBuilder sb) {
        if (parent != null) {
            parent.appendName(sb);
        }
        if (key instanceof String) {
            String strKey = (String) key;
            if (isIdentifierLike(strKey)) {
                if (sb.length() != 0) {
                    sb.append('.');
                }
                sb.append(key);
            } else {
                if (sb.length() == 0) {
                    sb.append("#ROOT");
                }
                sb.append('[').append(StringUtil.jQuote(key)).append(']');
            }
        } else if (key instanceof Number) {
            sb.append('[').append(key).append(']');
        }
    }

    private boolean isIdentifierLike(String s) {
        if (s.length() == 0) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isJavaIdentifierPart(c) && c != '-') {
                return false;
            }
        }
        return true;
    }
}
