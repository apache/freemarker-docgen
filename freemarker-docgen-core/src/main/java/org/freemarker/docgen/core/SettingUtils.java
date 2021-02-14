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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import freemarker.template.utility.StringUtil;

final class SettingUtils {
    private SettingUtils() {
        throw new AssertionError();
    }

    static DocgenException newCfgFileException(SettingName settingName, String desc) {
        return newCfgFileException(settingName, desc, null);
    }

    static DocgenException newCfgFileException(SettingName settingName, String desc, Throwable cause) {
        StringBuilder sb = new StringBuilder();
        sb.append("Wrong configuration");
        if (settingName != null) {
            sb.append(" setting \"").append(settingName).append("\"");
        }
        settingName.getContainingFile().ifPresent(containingFile -> sb.append(" in file \"").append(containingFile.getAbsolutePath()).append("\""));
        sb.append(":\n");
        sb.append(desc);
        return new DocgenException(sb.toString(), cause);
    }

    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> castSettingToMap(
            SettingName settingName, Object settingValue,
            Class<K> keyClass, Class<V> valueClass) {
        return castSettingToMap(settingName, settingValue, keyClass, valueClass, false);
    }

    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> castSettingToMap(
            SettingName settingName, Object settingValue,
            Class<K> keyClass, Class<V> valueClass, boolean allowNullValueInMap) {
        return (Map<K, V>) castSetting(
                settingName, settingValue,
                Map.class,
                new MapEntryType(keyClass, valueClass, allowNullValueInMap));
    }

    @SuppressWarnings("unchecked")
    static <T> List<T> castSettingToList(
            SettingName settingName,
            Object settingValue, Class<T> elementClass) {
        return castSetting(
                settingName, settingValue,
                null,
                List.class, new ListItemType(elementClass)
        );
    }

    static <T> T castSetting(SettingName settingName, Object settingValue, Class<T> valueType) {
        return castSetting(settingName, settingValue, null, valueType);
    }

    /**
     * Same as {@link #castSetting(List, Object, boolean, Class, List)} with {@code optional} {@code false}.
     */
    static <T> T castSetting(
            SettingName settingName, Object settingValue, Class<T> valueType,
            ContainedValueType... containedValueTypes) {
        return castSetting(settingName, settingValue, null, valueType, containedValueTypes);
    }

    /**
     * @param valueType
     *      The expected type of the value (on the top-level, if it's a container)
     * @param defaultValue
     *      {@code null} if the setting is required or can't have {@code null} value. Non-null otherwise.
     * @param containedValueTypes
     *      The expected type of the contained values, and of the values contained inside them, and so on. (This is
     *      separate from {@code valueType} because Java can't match s generic return type with the type of the first
     */
    static <T> T castSetting(
            SettingName settingName, Object settingValue,
            DefaultValue<T> defaultValue,
            Class<T> valueType, ContainedValueType... containedValueTypes) {
        if (settingValue == null) {
            if (defaultValue != null) {
                return defaultValue.get();
            }
            throw newNullSettingValueException(settingName);
        }
        if (!valueType.isInstance(settingValue)) {
            throw newBadSettingValueTypeException(settingName, valueType, settingValue);
        }

        checkContainedValueTypes(settingName, settingValue, containedValueTypes);

        return (T) settingValue;
    }

    static void checkContainedValueTypes(
            SettingName settingName, Object settingValue,
            ContainedValueType... containedValueTypes)  {
        if (containedValueTypes.length == 0) {
            return;
        }
        checkContainedValueTypes(settingName, settingValue, new ArrayList<>(containedValueTypes.length),
                containedValueTypes);
    }

    private static void checkContainedValueTypes(
            SettingName settingName, Object containerValue,
            List<Object> checkedContainedSettingNameTail, ContainedValueType... containedValueTypes) {
        if (checkedContainedSettingNameTail.size() == containedValueTypes.length || containerValue == null) {
            return;
        }

        Class<? extends Object> containerClass = containerValue.getClass();
        ContainedValueType containedValueType = containedValueTypes[checkedContainedSettingNameTail.size()];
        checkContainerClassIsValidContainedValueType(containerClass, containedValueType);
        if (containedValueType instanceof ListItemType) {
            int listElementIndex = 0;
            for (Object listElement : ((List<?>) containerValue)) {
                if (listElement == null) {
                    if (!containedValueType.allowNullValue) {
                        throw newNullSettingValueException(
                                settingName.subKey(checkedContainedSettingNameTail).subKey(listElementIndex));
                    }
                } else if (!containedValueType.valueType.isInstance(listElement)) {
                    throw newBadSettingValueTypeException(
                            settingName.subKey(checkedContainedSettingNameTail).subKey(listElementIndex),
                            containedValueType.valueType, listElement);
                }

                checkedContainedSettingNameTail.add(listElementIndex);
                try {
                    checkContainedValueTypes(
                            settingName, listElement, checkedContainedSettingNameTail,
                            containedValueTypes);
                } finally {
                    checkedContainedSettingNameTail.remove(checkedContainedSettingNameTail.size() - 1);
                }
                listElementIndex++;
            }
        } else if (containedValueType instanceof MapEntryType) {
            MapEntryType mapEntryType = (MapEntryType) containedValueType;
            for (Map.Entry<?, ?> mapEntry : ((Map<?, ?>) containerValue).entrySet()) {
                Object entryKey = mapEntry.getKey();
                if (entryKey == null) {
                    throw newCfgFileException(
                            settingName, "Null keys aren't allowed in this setting value.");
                }
                Class<?> keyType = mapEntryType.keyType;
                if (!keyType.isInstance(entryKey)) {
                    throw newCfgFileException(
                            settingName.subKey(checkedContainedSettingNameTail), // Don't add the key.
                            "Expected key type " + CJSONInterpreter.cjsonTypeNameForClass(keyType)
                                    + ", but key was of type " + CJSONInterpreter.cjsonTypeNameOfValue(entryKey));
                }

                Object entryValue = mapEntry.getValue();
                if (entryValue == null) {
                    if (!containedValueType.allowNullValue) {
                        throw newNullSettingValueException(
                                settingName.subKey(checkedContainedSettingNameTail).subKey(entryKey));
                    }
                } else if (!containedValueType.valueType.isInstance(entryValue)) {
                    throw newBadSettingValueTypeException(
                            settingName.subKey(checkedContainedSettingNameTail).subKey(entryKey),
                            containedValueType.valueType, entryValue);
                }

                checkedContainedSettingNameTail.add(entryKey);
                try {
                    checkContainedValueTypes(
                            settingName, entryValue, checkedContainedSettingNameTail,
                            containedValueTypes);
                } finally {
                    checkedContainedSettingNameTail.remove(checkedContainedSettingNameTail.size() - 1);
                }
            }
            if (mapEntryType.validateKeys) {
                checkMapKeys(settingName, (Map) containerValue, mapEntryType.requiredKeys, mapEntryType.optionalKeys);
            }
        } else {
            throw new AssertionError();
        }
    }

    private static void checkContainerClassIsValidContainedValueType(
            Class<?> containerClass, ContainedValueType containedValueType) {
        if (!containedValueType.isValidContainerClass(containerClass)) {
            throw new IllegalArgumentException(
                    containedValueType.getClass().getSimpleName()
                            + " is not fitting for provided container value class, "
                            + containerClass.getSimpleName() + ".");
        }
    }

    private static DocgenException newBadSettingValueTypeException(SettingName settingName, Class<?> expectedValueType,
            Object settingValue) throws
            DocgenException {
        return newCfgFileException(
                settingName,
                "Setting value should be a(n) " + CJSONInterpreter.cjsonTypeNameForClass(expectedValueType) + ", "
                        + "but was a(n) " + CJSONInterpreter.cjsonTypeNameOfValue(settingValue) + ".");
    }

    private static DocgenException newNullSettingValueException(SettingName settingName) {
        return newCfgFileException(
                settingName,
                "Setting is required but wasn't set (or was set to null).");
    }

    private static <T> void checkMapKeys(
            SettingName settingName, Map<T, ?> value,
            Set<T> requiredKeys, Set<T> optionalKeys) {
        Set<T> mapKeySet = value.keySet();
        for (T key : mapKeySet) {
            if (!requiredKeys.contains(key) && !optionalKeys.contains(key)) {
                throw newCfgFileException(settingName,
                        "Unsupported key in the map value: " + StringUtil.jQuote(key) + ". Valid keys are: "
                                + Sets.union(requiredKeys, optionalKeys).stream()
                                .sorted()
                                .map(it -> StringUtil.jQuote(it))
                                .collect(Collectors.joining(", ")));
            }
        }
        for (T requiredKey : requiredKeys) {
            if (!mapKeySet.contains(requiredKey)) {
                throw newCfgFileException(settingName, "Required key is missing from the map value: " + requiredKey);
            }
        }
    }

    abstract static class ContainedValueType {
        private final Class<?> valueType;
        private final boolean allowNullValue;

        private ContainedValueType(Class<?> valueType, boolean allowNullValue) {
            this.valueType = Objects.requireNonNull(valueType);
            this.allowNullValue = allowNullValue;
        }

        public abstract boolean isValidContainerClass(Class<?> containerClass);
    }

    final static class ListItemType extends ContainedValueType {
        public ListItemType(Class<?> valueType) {
            this(valueType, false);
        }

        public ListItemType(Class<?> valueType, boolean allowNullValue) {
            super(valueType, allowNullValue);
        }

        @Override
        public boolean isValidContainerClass(Class<?> containerClass) {
            return List.class.isAssignableFrom(containerClass);
        }
    }

    final static class MapEntryType<T> extends ContainedValueType {
        private final Class<T> keyType;
        private final boolean validateKeys;
        private final Set<T> requiredKeys;
        private final Set<T> optionalKeys;

        public MapEntryType(Class<T> keyType, Class<?> valueType) {
            this(keyType, valueType, false);
        }

        public MapEntryType(Class<T> keyType, Class<?> valueType, boolean allowNullValue) {
            this(keyType, false, Collections.emptySet(), Collections.emptySet(), valueType, allowNullValue);
        }

        public MapEntryType(
                Class<T> keyType, Set<T> requiredKeys,
                Class<?> valueType) {
            this(keyType, true, requiredKeys, Collections.emptySet(), valueType, false);
        }

        public MapEntryType(
                Class<T> keyType, Set<T> requiredKeys,
                Class<?> valueType, boolean allowNullValue) {
            this(keyType, true, requiredKeys, Collections.emptySet(), valueType, allowNullValue);
        }

        public MapEntryType(
                Class<T> keyType, Set<T> requiredKeys, Set<T> optionalKeys,
                Class<?> valueType) {
            this(keyType, true, requiredKeys, optionalKeys, valueType, false);
        }

        public MapEntryType(
                Class<T> keyType, Set<T> requiredKeys, Set<T> optionalKeys,
                Class<?> valueType, boolean allowNullValue) {
            this(keyType, true, requiredKeys, optionalKeys, valueType, allowNullValue);
        }

        private MapEntryType(
                Class<T> keyType, boolean validateKeys, Set<T> requiredKeys, Set<T> optionalKeys,
                Class<?> valueType, boolean allowNullValue) {
            super(valueType, allowNullValue);
            this.keyType = Objects.requireNonNull(keyType);
            this.validateKeys = validateKeys;
            this.requiredKeys = Objects.requireNonNull(requiredKeys);
            this.optionalKeys = Objects.requireNonNull(optionalKeys);
        }

        @Override
        public boolean isValidContainerClass(Class<?> containerClass) {
            return Map.class.isAssignableFrom(containerClass);
        }
    }

}
