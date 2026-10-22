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
package org.crebs.skpackets.elements.entity.section;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.shanebeee.skr.Registration;
import com.github.shanebeee.skr.skript.SimpleEntryValidator;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import org.bukkit.event.Event;
import org.crebs.skpackets.api.entity.metadata.EntityMeta;
import org.crebs.skpackets.api.util.field.ConstructionContext;
import org.crebs.skpackets.api.util.field.FieldAccessor;
import org.crebs.skpackets.api.util.field.FieldSchema;
import org.crebs.skpackets.elements.entity.field.meta.MetaFieldRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.entity.EntityData;
import org.skriptlang.skript.bukkit.entity.EntityUtils;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;

import java.util.*;
import java.util.stream.Collectors;

public class SecExprNewMeta extends SectionExpression<EntityMeta> {

    private static final Map<FieldSchema<EntityType, EntityMeta>, EntryValidator> VALIDATORS =
            new HashMap<>();

    public static void register(Registration reg) {

        for (FieldSchema<EntityType, EntityMeta> def : MetaFieldRegistry.INSTANCE.getAllSchemas()) {
            SimpleEntryValidator builder = SimpleEntryValidator.builder();
            for (FieldAccessor<EntityMeta, ?> field : def.accessors()) {
                builder.addOptionalEntry(field.name(), Object.class);

                for (String alias : field.aliases()) {
                    builder.addOptionalEntry(alias, Object.class);
                }
            }
            VALIDATORS.put(def, builder.build());
        }

        StringBuilder description = new StringBuilder();
        description.append("Create a new meta from an entity type.\n\n");
        description.append("### Available Metas and their fields\n");

        List<FieldSchema<EntityType, EntityMeta>> schemas =
                new ArrayList<>(MetaFieldRegistry.INSTANCE.getAllSchemas());
        schemas.sort(Comparator.comparingInt(schema -> schema.accessors().size()));
        for (FieldSchema<EntityType, EntityMeta> schema : schemas) {

            Set<String> myFields =
                    schema.accessors().stream()
                            .map(FieldAccessor::name)
                            .collect(Collectors.toSet());

            FieldSchema<EntityType, EntityMeta> parentSchema = null;
            int maxSubsetSize = -1;

            for (FieldSchema<EntityType, EntityMeta> other : schemas) {
                if (other == schema) {
                    continue;
                }
                Set<String> otherFields =
                        other.accessors().stream()
                                .map(FieldAccessor::name)
                                .collect(Collectors.toSet());

                if (myFields.containsAll(otherFields) && myFields.size() > otherFields.size()) {
                    if (otherFields.size() > maxSubsetSize) {
                        maxSubsetSize = otherFields.size();
                        parentSchema = other;
                    }
                }
            }

            Set<String> parentFieldNames =
                    parentSchema != null
                            ? parentSchema.accessors().stream()
                                    .map(FieldAccessor::name)
                                    .collect(Collectors.toSet())
                            : Collections.emptySet();

            StringBuilder fieldLines = new StringBuilder();
            for (FieldAccessor<EntityMeta, ?> field : schema.accessors()) {
                if (!parentFieldNames.contains(field.name())) {
                    fieldLines.append("  - `").append(field.name());
                    if (field.aliases().length > 0) {
                        fieldLines
                                .append(" (")
                                .append(String.join(", ", field.aliases()))
                                .append(")");
                    }

                    fieldLines.append("`\n");
                }
            }

            if (!fieldLines.isEmpty()) {
                String typeName =
                        schema.type()
                                .getName()
                                .getKey()
                                .toLowerCase(Locale.ENGLISH)
                                .replace("_", " ");
                description.append("* **").append(typeName).append("** fields:\n");

                if (parentSchema != null) {
                    String parentName =
                            parentSchema
                                    .type()
                                    .getName()
                                    .getKey()
                                    .toLowerCase(Locale.ENGLISH)
                                    .replace("_", " ");
                    description
                            .append("  - *(Inherits all fields from **")
                            .append(parentName)
                            .append("**)*\n");
                }

                description.append(fieldLines).append("\n");
            }
        }

        reg.newSimpleExpression(
                        SecExprNewMeta.class,
                        EntityMeta.class,
                        "[a] [new] [(fake|packet)] %*entitydata% [entity] meta [data]")
                .name("General - Create Meta")
                .description(description.toString())
                .since("1.0.0")
                .examples(
                        """
                        command /welcome-sign:
                            trigger:
                                set {_meta} to text display meta data:
                                    display content: "Hello!"
                                    display billboard: center
                                    display scale: vector(2, 2, 2)
                                set {_entity} to a new fake text display entity:
                                    location: location of player
                                    viewers: player
                                    meta: {_meta}
                                wait 5 seconds
                                kill fake entity {_entity}
                        """)
                .register();
    }

    private final Map<String, Expression<?>> fieldExpressions = new HashMap<>();

    private EntityType type;
    private FieldSchema<EntityType, EntityMeta> schema;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(
            Expression<?>[] expressions,
            int matchedPattern,
            Kleenean isDelayed,
            SkriptParser.ParseResult parseResult,
            @Nullable SectionNode sectionNode,
            @Nullable List<TriggerItem> triggerItems) {

        if (!(expressions[0] instanceof Literal<?>)) {
            Skript.error("The entity type needs to be a literal");
            return false;
        }

        Literal<EntityData<?>> entityDataLiteral = (Literal<EntityData<?>>) expressions[0];

        EntityData<?> skriptType = entityDataLiteral.getSingle();
        if (skriptType == null) {
            return false;
        }

        org.bukkit.entity.EntityType bukkitType = EntityUtils.toBukkitEntityType(skriptType);
        this.type = SpigotConversionUtil.fromBukkitEntityType(bukkitType);

        this.schema = MetaFieldRegistry.INSTANCE.getSchema(this.type);

        if (this.schema == null) {
            Skript.warning(
                    "Meta creation for "
                            + bukkitType.toString().toLowerCase(Locale.ENGLISH).replace("_", " ")
                            + " is not currently supported. Consider creating/handling it through"
                            + " reflection.");
            return true;
        }

        if (this.schema.constructor() == null) {
            Skript.error(
                    "Meta creation for "
                            + bukkitType.toString().toLowerCase(Locale.ENGLISH).replace("_", " ")
                            + " is abstract and thus can't be created. Use an extending entity"
                            + " instead.");
            return false;
        }

        boolean hasRequiredFields =
                this.schema.accessors().stream().anyMatch(field -> !field.isOptional());
        if (sectionNode == null) {
            if (hasRequiredFields) {
                Skript.error(
                        "You must provide a section with the required fields to create a "
                                + bukkitType
                                        .toString()
                                        .toLowerCase(Locale.ENGLISH)
                                        .replace("_", " ")
                                + " meta.");
                return false;
            }
            return true;
        }

        EntryValidator validator = VALIDATORS.get(this.schema);
        if (validator == null) {
            Skript.error(
                    "No validator found for "
                            + bukkitType.toString().toLowerCase(Locale.ENGLISH).replace("_", " ")
                            + " meta.");
            return false;
        }

        EntryContainer container = validator.validate(sectionNode);
        if (container == null) {
            return false;
        }

        List<String> missingKeys = new ArrayList<>();
        for (FieldAccessor<EntityMeta, ?> field : this.schema.accessors()) {
            String key = field.name();
            Class<?> expectedType = field.expectedType();

            Expression<?> expr = container.getOptional(key, Object.class, false);

            if (expr == null) {
                for (String alias : field.aliases()) {
                    expr = container.getOptional(alias, Object.class, false);
                    if (expr != null) {
                        break;
                    }
                }
            }

            if (expr == null) {
                if (!field.isOptional()) {
                    missingKeys.add(key);
                }
                continue;
            }

            Class<?> baseType =
                    expectedType.isArray() ? expectedType.getComponentType() : expectedType;
            Expression<?> converted = expr.getConvertedExpression(baseType);

            if (converted == null) {
                Skript.error(
                        "The value for '"
                                + key
                                + "' must be of type "
                                + expectedType.getSimpleName()
                                + ".");
                return false;
            }

            this.fieldExpressions.put(key, converted);
        }

        if (!missingKeys.isEmpty()) {
            String metaName =
                    this.type.getName().getKey().toLowerCase(Locale.ENGLISH).replace("_", " ");
            Skript.error(
                    "Missing required entries for "
                            + metaName
                            + " meta: "
                            + String.join(", ", missingKeys));
            return false;
        }

        return true;
    }

    @Override
    protected EntityMeta @Nullable [] get(Event event) {
        return new EntityMeta[] {createMeta(event)};
    }

    private @Nullable EntityMeta createMeta(@NotNull Event event) {
        if (this.schema == null) {
            return null;
        }

        Map<String, Object> values = new HashMap<>();
        for (FieldAccessor<EntityMeta, ?> field : this.schema.accessors()) {
            Expression<?> expr = this.fieldExpressions.get(field.name());

            if (expr == null) {
                continue;
            }

            Object value;
            if (field.expectedType().isArray() || !expr.isSingle()) {
                Object[] array = expr.getArray(event);
                value = (array == null || array.length == 0) ? null : array;
            } else {
                value = expr.getSingle(event);
            }

            if (value == null) {
                if (!field.isOptional()) {
                    return null;
                }
                continue;
            }
            values.put(field.name(), value);
        }

        return this.schema
                .constructor()
                .apply(new ConstructionContext<>(this.type, this.schema.accessors(), values));
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends EntityMeta> getReturnType() {
        return EntityMeta.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String entityData =
                (this.type != null
                        ? this.type.getName().getKey().toLowerCase(Locale.ENGLISH).replace("_", " ")
                        : "unknown");
        return String.format("a new %s meta", entityData);
    }
}
