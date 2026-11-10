package org.crebs.skpackets.elements.general.field;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.github.shanebeee.skr.skript.SimpleEntryValidator;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.util.field.*;

import java.lang.reflect.Type;
import java.util.*;

public final class PacketFieldSections {
    private PacketFieldSections() {}

    public static boolean parse(List<? extends FieldAccessor<?, ?>> fields, SectionNode node,
                                Map<String, Expression<?>> expressions, String description) {
        var validator = SimpleEntryValidator.builder();
        for (var field : fields) {
            List<String> names = new ArrayList<>(List.of(field.name()));
            names.addAll(Arrays.asList(field.aliases()));
            for (String name : names) {
                if (node.get(name) instanceof SectionNode) validator.addOptionalSection(name);
                else validator.addOptionalEntry(name, Object.class);
            }
        }
        var entries = validator.build().validate(node);
        if (entries == null) return false;
        Set<String> nested = new HashSet<>();
        for (var field : fields) {
            List<String> names = new ArrayList<>(List.of(field.name()));
            names.addAll(Arrays.asList(field.aliases()));
            List<String> present = names.stream().filter(entries::hasEntry).toList();
            if (present.size() > 1) return error("Duplicate entries for packet field '" + field.name() + "': " + present);
            if (present.isEmpty() || !(node.get(present.getFirst()) instanceof SectionNode section)) continue;
            Expression<?> value = nested(field.nativeType(), section, description + "." + field.name());
            if (value == null) return false;
            expressions.put(field.name(), value);
            nested.add(field.name());
        }
        if (!FieldSections.parse(fields, entries, expressions, description, true, nested)) return false;
        for (var field : fields) {
            Class<?> nativeType = PacketArguments.raw(PacketRecords.elementType(field.nativeType()));
            Class<?> publicType = field.expectedType().isArray() ? field.expectedType().getComponentType() : field.expectedType();
            Expression<?> expression = expressions.get(field.name());
            if (!nativeType.isEnum() || publicType != String.class || expression == null) continue;
            if (!(expression instanceof ch.njol.skript.lang.Literal<?>)
                    && !(expression instanceof ch.njol.skript.lang.VariableString text && text.isSimple())) continue;
            try {
                for (Object value : expression.getArray(null)) PacketArguments.convert(value, nativeType, field.name());
            } catch (IllegalArgumentException exception) {
                return error(exception.getMessage());
            }
        }
        return true;
    }

    private static Expression<?> nested(Type type, SectionNode section, String name) {
        PacketRecords.Shape shape = PacketRecords.shape(type);
        if (shape == null) {
            error("Packet field '" + name + "' does not support nested entries. Use a value copied from a packet.");
            return null;
        }
        List<Map<String, Expression<?>>> records = new ArrayList<>();
        if (PacketRecords.isList(type)) {
            Set<String> labels = new HashSet<>();
            for (var node : section) {
                if (node.isVoid()) continue;
                if (!(node instanceof SectionNode child)) {
                    error("'" + name + "' requires a section per list entry."); return null;
                }
                if (!labels.add(child.getKey().toLowerCase(Locale.ROOT))) {
                    error("Duplicate list entry in '" + name + "': " + child.getKey()); return null;
                }
                Map<String, Expression<?>> record = new LinkedHashMap<>();
                if (!parse(shape.fields(), child, record, name + "." + child.getKey())) return null;
                records.add(record);
            }
        } else {
            Map<String, Expression<?>> record = new LinkedHashMap<>();
            if (!parse(shape.fields(), section, record, name)) return null;
            records.add(record);
        }
        return new RecordsExpression(shape, records, !PacketRecords.isList(type));
    }

    private static boolean error(String message) { Skript.error(message); return false; }

    private static final class RecordsExpression extends SimpleExpression<PacketValue> {
        private final PacketRecords.Shape shape;
        private final List<Map<String, Expression<?>>> records;
        private final boolean single;
        RecordsExpression(PacketRecords.Shape shape, List<Map<String, Expression<?>>> records, boolean single) {
            this.shape = shape; this.records = records; this.single = single;
        }
        @Override public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, SkriptParser.ParseResult result) { return true; }
        @Override protected PacketValue[] get(Event event) {
            List<PacketValue> values = new ArrayList<>();
            var schema = new FieldSchema<String, Object>("nested packet value", shape.fields(), context -> {
                Map<String, Object> fields = new LinkedHashMap<>();
                for (var field : shape.fields()) {
                    Object value = context.getOptional(field.name(), Object.class);
                    if (value != null) fields.put(field.name(), value);
                }
                return new PacketValue(new PacketRecord(fields));
            });
            for (var record : records) {
                Object value = FieldSections.create(schema, "nested packet value", record, event, Map.of());
                if (value == null) throw new IllegalArgumentException("A required nested packet entry evaluated to no value.");
                values.add((PacketValue)value);
            }
            return values.toArray(PacketValue[]::new);
        }
        @Override public boolean isSingle() { return single; }
        @Override public Class<PacketValue> getReturnType() { return PacketValue.class; }
        @Override public String toString(Event event, boolean debug) { return "nested packet fields"; }
    }
}
