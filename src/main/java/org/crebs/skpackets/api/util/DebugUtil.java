/*
 * MIT License
 *
 * Copyright (c) 2026 3add
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.crebs.skpackets.api.util;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class DebugUtil {

    private static final int MAX_DEPTH = 5;
    private static final String INDENT_STEP = "  ";

    private static final ClassValue<List<Method>> GETTERS = new ClassValue<>() {
        @Override protected List<Method> computeValue(Class<?> type) {
            return Arrays.stream(type.getDeclaredMethods())
                    .filter(method -> !method.isAnnotationPresent(Deprecated.class))
                    .filter(method -> method.getParameterCount() == 0)
                    .filter(method -> method.getName().startsWith("get") || method.getName().startsWith("is"))
                    .filter(method -> !method.getName().contains("Handle")
                            && !method.getName().contains("copy")
                            && !method.getName().contains("toString")
                            && !method.getName().contains("PacketEventsData"))
                    .filter(Method::trySetAccessible)
                    .toList();
        }
    };

    public static String getDebugString(Object obj) {
        if (obj == null) {
            return "<none>";
        }

        StringBuilder output = new StringBuilder();

        output.append(obj.getClass().getSimpleName()).append(":\n");

        appendFields(obj, 1, output);

        return output.toString().stripTrailing();
    }

    private static void appendFields(Object obj, int depth, StringBuilder output) {
        if (obj == null || depth > MAX_DEPTH) {
            return;
        }

        String indent = INDENT_STEP.repeat(depth);
        Class<?> currentClass = obj.getClass();

        try {
            for (Method method : GETTERS.get(currentClass)) {
                processValue(cleanFieldName(method.getName()), method.invoke(obj), depth, output);
            }
        } catch (Exception e) {
            output.append(indent)
                    .append("[Reflection Error: ")
                    .append(e.getClass().getSimpleName())
                    .append(" - ")
                    .append(e.getMessage())
                    .append("]\n");
        }
    }

    private static void processValue(
            String fieldName, Object value, int depth, StringBuilder output) {
        String indent = INDENT_STEP.repeat(depth);

        if (value == null) {
            output.append(indent).append(fieldName).append(": <none>\n");
            return;
        }

        if (value instanceof Optional<?> optional) {
            processValue(fieldName, optional.orElse(null), depth, output);
            return;
        }

        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                output.append(indent).append(fieldName).append(": <empty map>\n");
            } else if (depth >= MAX_DEPTH) {
                output.append(indent).append(fieldName).append(": ").append(map).append("\n");
            } else {
                output.append(indent).append(fieldName).append(":\n");
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String entryName = valueToString(entry.getKey());
                    processValue(entryName, entry.getValue(), depth + 1, output);
                }
            }
        } else if (value instanceof Iterable<?> || value.getClass().isArray()) {
            List<Object> list = new ArrayList<>();
            if (value instanceof Iterable<?> iterable) {
                iterable.forEach(list::add);
            } else {
                int length = Array.getLength(value);
                for (int i = 0; i < length; i++) {
                    list.add(Array.get(value, i));
                }
            }

            boolean isMetadataList = list.stream().anyMatch(EntityData.class::isInstance);

            if (isMetadataList) {
                output.append(indent).append(fieldName).append(":\n");
                for (Object item : list) {
                    output.append(indent).append(INDENT_STEP);
                    appendMetadataEntry(item, output);
                }
            } else {
                if (list.isEmpty()) {
                    output.append(indent).append(fieldName).append(": <empty list>\n");
                } else if (depth >= MAX_DEPTH || list.stream().allMatch(DebugUtil::isSimpleValue)) {
                    String listContent =
                            list.stream()
                                    .map(DebugUtil::valueToString)
                                    .collect(Collectors.joining(", "));
                    output.append(indent)
                            .append(fieldName)
                            .append(": [")
                            .append(listContent)
                            .append("]\n");
                } else {
                    output.append(indent).append(fieldName).append(":\n");
                    for (int i = 0; i < list.size(); i++) {
                        processValue("- " + i, list.get(i), depth + 1, output);
                    }
                }
            }

        } else if (!isSimpleValue(value)) {

            if (depth < MAX_DEPTH) {
                output.append(indent).append(fieldName).append(":\n");
                appendFields(value, depth + 1, output);
            } else {
                output.append(indent)
                        .append(fieldName)
                        .append(": ")
                        .append(valueToString(value))
                        .append("\n");
            }

        } else {
            output.append(indent)
                    .append(fieldName)
                    .append(": ")
                    .append(valueToString(value))
                    .append("\n");
        }
    }

    private static String cleanFieldName(String methodName) {
        String fieldName = methodName;
        if (fieldName.startsWith("get")) {
            fieldName = fieldName.substring(3);
        } else if (fieldName.startsWith("is")) {
            fieldName = fieldName.substring(2);
        }

        StringBuilder skriptName = new StringBuilder();
        for (char c : fieldName.toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (!skriptName.isEmpty()) {
                    skriptName.append(" ");
                }
                skriptName.append(Character.toLowerCase(c));
            } else {
                skriptName.append(c);
            }
        }
        return skriptName.toString();
    }

    private static boolean isSimpleValue(Object value) {
        if (value == null) {
            return true;
        }
        Class<?> cls = value.getClass();

        if (cls.isPrimitive()
                || cls == String.class
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof UUID
                || cls.isEnum()) {
            return true;
        }

        String className = cls.getName();
        return className.contains("Component")
                || className.endsWith("EntityType")
                || className.endsWith("EntityDataType")
                || className.endsWith("EntityPose")
                || className.endsWith("Vector3d")
                || className.endsWith("Vector3i")
                || className.endsWith("Location")
                || className.endsWith("ItemStack")
                || className.endsWith("ProtocolVersion")
                || className.endsWith("ItemType")
                || className.endsWith("ResourceLocation")
                || className.endsWith("NamespacedKey");
    }

    private static String valueToString(Object value) {
        return switch (value) {
            case null -> "<none>";
            case Optional<?> optional -> optional.map(DebugUtil::valueToString).orElse("<none>");
            case Enum<?> anEnum -> anEnum.name().toLowerCase(Locale.ENGLISH).replace("_", " ");
            default -> {
                if (value.getClass().isArray()) {
                    List<String> list = new ArrayList<>();
                    int len = Array.getLength(value);
                    for (int i = 0; i < len; i++) {
                        list.add(valueToString(Array.get(value, i)));
                    }
                    yield "[" + String.join(", ", list) + "]";
                }
                yield value.toString();
            }
        };
    }

    private static void appendMetadataEntry(Object data, StringBuilder builder) {
        if (data == null) {
            builder.append("<none> metadata entry\n");
            return;
        }

        if (!(data instanceof EntityData<?> metadata)) {
            builder.append("Unparsable Data: ").append(data).append("\n");
            return;
        }

        String typeString = Objects.toString(metadata.getType());
        int start = typeString.indexOf('[');
        int end = typeString.indexOf(']');
        if (start != -1 && end != -1 && end > start) {
            typeString = typeString.substring(start + 1, end);
        }

        builder.append("[")
                .append(metadata.getIndex())
                .append(": ")
                .append(typeString.toLowerCase(Locale.ENGLISH))
                .append(" = ")
                .append(valueToString(metadata.getValue()))
                .append("]\n");
    }
}
