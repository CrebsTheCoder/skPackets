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
package org.crebs.skpackets.elements.entity.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

import com.github.shanebeee.skr.Registration;

import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.wrapper.WrapperEntity;

import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ExprFakeEntityFromUuid extends SimpleExpression<WrapperEntity> {

    public static void register(Registration reg) {
        reg.newSimpleExpression(
                        ExprFakeEntityFromUuid.class,
                        WrapperEntity.class,
                        "(fake|packet)[ ]entity (from|with) uuid %uuid%")
                .name("Fake Entity - From Entity UUID")
                .description(
                        "Retrieve a fake entity from it's entity uuid, this only works for spawned"
                                + " fake entities")
                .examples(
                        """
                        command /find-my-clone:
                            trigger:
                                set {_entity} to a new fake zombie entity:
                                    location: location of player
                                    viewers: player
                                set {_key} to fake entity uuid of {_entity}
                                set {_found} to fake entity with uuid {_key}
                                send "Found %{_found}%" to player
                                wait 5 seconds
                                kill fake entity {_entity}
                        """)
                .since("1.0.0")
                .register();
    }

    private Expression<UUID> uuidExpr;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(
            Expression<?>[] expressions,
            int matchedPattern,
            Kleenean isDelayed,
            SkriptParser.ParseResult parseResult) {
        this.uuidExpr = (Expression<UUID>) expressions[0];
        return true;
    }

    @Override
    protected WrapperEntity @Nullable [] get(Event event) {
        UUID uuid = this.uuidExpr.getSingle(event);
        if (uuid == null) return null;
        WrapperEntity entity =
                EntityLib.get().getEntities().stream()
                        .filter(candidate -> candidate.getUuid().equals(uuid))
                        .findFirst()
                        .orElse(null);
        return entity == null ? null : new WrapperEntity[] {entity};
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
        String uuid = this.uuidExpr.toString(event, debug);
        return String.format("fake entity with uuid %s", uuid);
    }
}
