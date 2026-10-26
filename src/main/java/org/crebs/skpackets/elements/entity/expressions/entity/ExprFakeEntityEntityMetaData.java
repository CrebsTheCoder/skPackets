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
package org.crebs.skpackets.elements.entity.expressions.entity;

import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.shanebeee.skr.Registration;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.entity.metadata.EntityMeta;

import java.util.ArrayList;

public final class ExprFakeEntityEntityMetaData extends PropertyExpression<Object, EntityMeta> {
    public static void register(Registration registration) {
        registration.newPropertyExpression(ExprFakeEntityEntityMetaData.class, EntityMeta.class,
                        "(fake|packet) entity [field] (meta [data]|entity meta data)", "packetentities/entities")
                .name("Fake Entity - Entity Meta Data")
                .description("Returns live metadata for fake entities, or a metadata snapshot for real entities. "
                        + "Changing a snapshot does not change the server entity: send it in an entity metadata packet to chosen viewers. "
                        + "For persistent player glow, use 'set packet glow state of player to true for player'.")
                .examples("""
                        command /metadata-glow:
                            trigger:
                                set {_meta} to fake entity entity meta data of player
                                set meta glowing state of {_meta} to true
                                set {_packet} to a new clientbound entity metadata packet:
                                    entity id: protocol id of player
                                    entity metadata: {_meta}
                                silently send packet {_packet} to player
                        """, "set packet glow state of player to true for player")
                .since("1.0.0").register();
    }

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, SkriptParser.ParseResult result) {
        setExpr(expressions[0]);
        return true;
    }

    @Override
    protected EntityMeta[] get(Event event, Object[] source) {
        var values = new ArrayList<EntityMeta>();
        for (Object value : source) {
            if (value instanceof WrapperEntity fake) values.add(EntityMeta.of(fake));
            else if (value instanceof Entity entity) {
                EntityMeta meta = EntityMeta.createMeta(entity.getEntityId(), SpigotConversionUtil.fromBukkitEntityType(entity.getType()));
                meta.readPacket(new WrapperPlayServerEntityMetadata(entity.getEntityId(), SpigotConversionUtil.getEntityMetadata(entity)));
                values.add(meta);
            }
        }
        return values.toArray(EntityMeta[]::new);
    }

    @Override
    public Class<? extends EntityMeta> getReturnType() { return EntityMeta.class; }

    @Override
    public String toString(Event event, boolean debug) {
        return "fake entity meta data of " + getExpr().toString(event, debug);
    }
}
