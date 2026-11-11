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
package org.crebs.skpackets.elements.general.section;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.shanebeee.skr.Registration;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.util.field.FieldSchema;
import org.crebs.skpackets.api.util.field.FieldSections;
import org.crebs.skpackets.elements.general.field.PacketFieldRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SecExprNewPacket extends SectionExpression<PacketWrapper<?>> {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register(Registration reg) {
        String description = "Create a new packet from a packet type. Fields use native Skript values; "
                + "packet-specific enums use quoted text. Use vec2(x, z) for two-dimensional coordinates. Equipment, player-info entries, profiles and trades support nested sections. Other complex values can be copied from packets using the shared packet value type. Optional fields may be omitted. Types without a public "
                + "PacketEvents data constructor expose raw data as a list of byte numbers. "
                + "Sending a packet still requires the matching protocol version and connection state.\n"
                + FieldSchema.describe(PacketFieldRegistry.INSTANCE.getAllSchemas(),
                type -> new org.crebs.skpackets.elements.general.Types.PacketTypeParser().toString(type, 0));

        reg.newSimpleExpression(
                        SecExprNewPacket.class,
                        (Class) PacketWrapper.class,
                        "[a] [new] %*packettype%")
                .name("General - New Packet")
                .description(description)
                .examples(
                        """
                        command killTargetForMe:
                            trigger:
                                set {_packet} to a new clientbound destroy entities packet:
                                    entity ids: protocol id of target entity

                                silently send packet {_packet} to the player
                        """,
                        """
                        command /start-cracking:
                            trigger:
                                set {_block} to target block of player
                                set {_pos} to vector(x-coordinate of {_block}, y-coordinate of {_block}, z-coordinate of {_block})
                                set {_random} to random integer between 100000 and 999999
                                loop 9 times:
                                    set {_packet} to a new clientbound block break animation packet:
                                        entity id: {_random}
                                        block position: {_pos}
                                        destroy stage: loop-number
                                    silently send packet {_packet} to player
                                    wait 1 tick
                                set {_packet} to a new clientbound block break animation packet:
                                    entity id: {_random}
                                    block position: {_pos}
                                    destroy stage: -1
                                silently send packet {_packet} to player
                        """)
                .since("1.0.0")
                .register();
    }

    private final Map<String, Expression<?>> fieldExpressions = new HashMap<>();

    private PacketTypeCommon type;
    private FieldSchema<PacketTypeCommon, PacketWrapper<?>> schema;

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
            Skript.error("The packet type needs to be a literal");
            return false;
        }

        Literal<PacketTypeCommon> packetTypeLiteral = (Literal<PacketTypeCommon>) expressions[0];

        this.type = packetTypeLiteral.getSingle();
        this.schema = PacketFieldRegistry.INSTANCE.getSchema(this.type);

        if (this.schema == null || this.schema.constructor() == null) {
            Skript.error(
                    "Packet creation for "
                            + this.type.getName().toLowerCase(Locale.ENGLISH).replace("_", " ")
                            + " is not currently supported. Consider creating/handling it through"
                            + " reflection.");
            return false;
        }

        boolean hasRequiredFields =
                this.schema.accessors().stream().anyMatch(field -> !field.isOptional());
        if (sectionNode == null) {
            if (hasRequiredFields) {
                Skript.error(
                        "You must provide a section with the required fields to create a "
                                + this.type.getName().toLowerCase(Locale.ENGLISH).replace("_", " ")
                                + " packet.");
                return false;
            }

            return true;
        }

        return org.crebs.skpackets.elements.general.field.PacketFieldSections.parse(
                this.schema.accessors(), sectionNode, this.fieldExpressions,
                this.type.getName().toLowerCase(Locale.ENGLISH).replace("_", " ") + " packet");
    }

    @Override
    protected PacketWrapper<?> @Nullable [] get(Event event) {
        try {
            PacketWrapper<?> packet = createPacket(event);
            return packet == null ? new PacketWrapper[0] : new PacketWrapper[] {packet};
        } catch (IllegalArgumentException | IllegalStateException ex) {
            error(ex.getMessage());
            return new PacketWrapper[0];
        }
    }

    private @Nullable PacketWrapper<?> createPacket(@NotNull Event event) {
        return FieldSections.create(this.schema, this.type, this.fieldExpressions, event, Map.of());
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Class<? extends PacketWrapper<?>> getReturnType() {
        return (Class) PacketWrapper.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String packetType =
                (this.type != null
                        ? this.type.getName().toLowerCase(Locale.ENGLISH).replace("_", " ")
                        : "unknown");
        return String.format("a new %s packet", packetType);
    }
}
