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
import com.github.shanebeee.skr.Registration;
import com.github.shanebeee.skr.skript.SimpleEntryValidator;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.util.field.FieldAccessor;
import org.crebs.skpackets.api.util.field.FieldSchema;
import org.crebs.skpackets.api.util.field.FieldSections;
import org.crebs.skpackets.elements.entity.field.entity.FakeEntityFieldRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;

import java.util.*;

public class SecExprNewFakeEntity extends SectionExpression<WrapperEntity> {

    private static final Map<FieldSchema<EntityType, WrapperEntity>, EntryValidator> VALIDATORS =
            new HashMap<>();

    public static void register(Registration reg) {

        for (FieldSchema<EntityType, WrapperEntity> def :
                FakeEntityFieldRegistry.INSTANCE.getAllSchemas()) {
            SimpleEntryValidator builder = SimpleEntryValidator.builder();
            for (FieldAccessor<WrapperEntity, ?> field : def.accessors()) {
                if (field.name().equals("persistent")) continue;
                builder.addOptionalEntry(field.name(), Object.class);

                for (String alias : field.aliases()) {
                    builder.addOptionalEntry(alias, Object.class);
                }
            }
            VALIDATORS.put(def, builder.build());
        }

        String description = "Creates a fake entity; fake and packet are aliases. The typed form requires location and viewers, "
                + "and optionally accepts meta, entity id and entity uuid. "
                + "Add 'with persistent' to keep the entity across viewer reconnects and world changes. "
                + "Player entities also accept username and skin. Use the typed form (new fake zombie entity), "
                + "or new fake entity with an entity entry. The latter also accepts a meta field section.";

        reg.newSimpleExpression(
                        SecExprNewFakeEntity.class,
                        WrapperEntity.class,
                        "[a] [new] (fake|packet) %*entitydata% entity [persistent:with persistent]",
                        "[a] new (fake|packet) entity [persistent:with persistent]")
                .name("Fake Entity - Create Fake Entity")
                .description(description)
                .examples(
                        """
                        command /my-double:
                            trigger:
                                set {_entity} to a new fake player entity:
                                    location: location of player
                                    viewers: player
                                    username: "Demo"
                                    skin: skin of player
                                wait 5 seconds
                                kill fake entity {_entity}
                        """)
                .since("1.0.0")
                .register();
    }

    private final Map<String, Expression<?>> fieldExpressions = new HashMap<>();

    private final PacketEntitySpawn spawn = new PacketEntitySpawn();
    private boolean generic;
    private boolean persistent;
    private EntityType type;
    private FieldSchema<EntityType, WrapperEntity> schema;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(
            Expression<?>[] expressions,
            int matchedPattern,
            Kleenean isDelayed,
            SkriptParser.ParseResult parseResult,
            @Nullable SectionNode sectionNode,
            @Nullable List<TriggerItem> triggerItems) {
        persistent = parseResult.hasTag("persistent");
        generic = matchedPattern == 1;
        if (generic) return spawn.init(sectionNode, persistent);

        org.bukkit.entity.EntityType bukkitType = FieldSections.entityType(expressions[0]);
        if (bukkitType == null) return false;
        String entityTypeName = bukkitType.toString().toLowerCase(Locale.ENGLISH).replace("_", " ");
        this.type = SpigotConversionUtil.fromBukkitEntityType(bukkitType);

        this.schema = FakeEntityFieldRegistry.INSTANCE.getSchema(this.type);

        if (this.schema == null) {
            Skript.warning(
                    "Fake entity creation for "
                            + entityTypeName
                            + " is not currently supported. Consider creating/handling it through"
                            + " reflection.");
            return true;
        }

        boolean hasRequiredFields =
                this.schema.accessors().stream().anyMatch(field -> !field.isOptional());
        if (sectionNode == null) {
            if (hasRequiredFields) {
                Skript.error(
                        "You must provide a section with the required fields to create a "
                                + entityTypeName
                                + " fake entity.");
                return false;
            }
            return true;
        }

        EntryValidator validator = VALIDATORS.get(this.schema);
        if (validator == null) {
            Skript.error(
                    "No validator found for "
                            + entityTypeName
                            + " entity.");
            return false;
        }

        EntryContainer container = validator.validate(sectionNode);
        if (container == null) {
            return false;
        }

        return FieldSections.parse(this.schema.accessors(), container, this.fieldExpressions,
                entityTypeName + " entity", false, Set.of("persistent"));
    }

    @Override
    protected WrapperEntity @Nullable [] get(Event event) {
        WrapperEntity entity = generic ? spawn.spawn(event) : createEntity(event);
        return entity == null ? new WrapperEntity[0] : new WrapperEntity[] {entity};
    }

    private @Nullable WrapperEntity createEntity(@NotNull Event event) {
        return FieldSections.create(this.schema, this.type, this.fieldExpressions, event, Map.of("persistent", persistent));
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends WrapperEntity> getReturnType() {
        return WrapperEntity.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String type = this.type.getName().getKey().toLowerCase(Locale.ENGLISH).replace("_", " ");
        return String.format("a new fake %s entity", type);
    }
}
