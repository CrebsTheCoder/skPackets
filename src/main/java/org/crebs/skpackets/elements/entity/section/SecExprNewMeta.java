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
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.shanebeee.skr.Registration;
import com.github.shanebeee.skr.skript.SimpleEntryValidator;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.entity.metadata.EntityMeta;
import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.api.util.field.FieldAccessor;
import org.crebs.skpackets.api.util.field.FieldSchema;
import org.crebs.skpackets.api.util.field.FieldSections;
import org.crebs.skpackets.elements.entity.field.meta.MetaFieldRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;

import java.util.*;

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

        StringBuilder description = new StringBuilder("Creates metadata for an entity type. "
                + "Every entity inherits the base entity fields. Living entities add health; "
                + "displays share display fields and add fields for their content.\n\n");
        var base = EntityTypes.ARMOR_STAND;
        appendFields(description, "All entities", base, field -> field.supports(
                EntityTypes.ITEM)
                && field.supports(EntityTypes.TEXT_DISPLAY));
        appendFields(description, "Living entities (including players)", base, field -> field == PacketEntityField.HEALTH);
        appendFields(description, "Dropped items", EntityTypes.ITEM, field -> field == PacketEntityField.ITEM);
        appendFields(description, "Interactions", EntityTypes.INTERACTION, field -> field.name().startsWith("INTERACTION_"));
        var block = EntityTypes.BLOCK_DISPLAY;
        var item = EntityTypes.ITEM_DISPLAY;
        var text = EntityTypes.TEXT_DISPLAY;
        appendFields(description, "All displays (plus base entity fields)", text,
                field -> !field.supports(base) && field.supports(block) && field.supports(item));
        appendFields(description, "Block displays (plus display fields)", block,
                field -> !field.supports(base) && !field.supports(text));
        appendFields(description, "Item displays (plus display fields)", item,
                field -> !field.supports(base) && !field.supports(text));
        appendFields(description, "Text displays (plus display fields)", text,
                field -> !field.supports(base) && !field.supports(block));

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

    private static void appendFields(StringBuilder description, String title, EntityType type,
            java.util.function.Predicate<PacketEntityField> include) {
        description.append("**").append(title).append("**\n");
        for (var field : PacketEntityField.values()) {
            if (field.supports(type) && include.test(field))
                description.append("- ").append(String.join(" / ", field.aliases())).append("\n");
        }
        description.append("\n");
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

        org.bukkit.entity.EntityType bukkitType = FieldSections.entityType(expressions[0]);
        if (bukkitType == null) return false;
        String entityTypeName = bukkitType.toString().toLowerCase(Locale.ENGLISH).replace("_", " ");
        this.type = SpigotConversionUtil.fromBukkitEntityType(bukkitType);

        this.schema = MetaFieldRegistry.INSTANCE.getSchema(this.type);

        if (this.schema == null) {
            Skript.warning(
                    "Meta creation for "
                            + entityTypeName
                            + " is not currently supported. Consider creating/handling it through"
                            + " reflection.");
            return true;
        }

        if (this.schema.constructor() == null) {
            Skript.error(
                    "Meta creation for "
                            + entityTypeName
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
                                + entityTypeName
                                + " meta.");
                return false;
            }
            return true;
        }

        EntryValidator validator = VALIDATORS.get(this.schema);
        if (validator == null) {
            Skript.error(
                    "No validator found for "
                            + entityTypeName
                            + " meta.");
            return false;
        }

        EntryContainer container = validator.validate(sectionNode);
        if (container == null) {
            return false;
        }

        return FieldSections.parse(this.schema.accessors(), container, this.fieldExpressions,
                entityTypeName + " meta", false, Set.of());
    }

    @Override
    protected EntityMeta @Nullable [] get(Event event) {
        EntityMeta meta = createMeta(event);
        return meta == null ? new EntityMeta[0] : new EntityMeta[] {meta};
    }

    private @Nullable EntityMeta createMeta(@NotNull Event event) {
        return FieldSections.create(this.schema, this.type, this.fieldExpressions, event, Map.of());
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
