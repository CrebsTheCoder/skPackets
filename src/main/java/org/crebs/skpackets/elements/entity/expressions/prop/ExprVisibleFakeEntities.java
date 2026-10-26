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
package org.crebs.skpackets.elements.entity.expressions.prop;

import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;

import com.github.shanebeee.skr.Registration;

import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.wrapper.WrapperEntity;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ExprVisibleFakeEntities extends PropertyExpression<Player, WrapperEntity> {

    public static void register(Registration reg) {
        reg.newPropertyExpression(
                        ExprVisibleFakeEntities.class,
                        WrapperEntity.class,
                        "[visible] (fake|packet)[ ]entities",
                        "player")
                .name("Fake Entity - Visible Fake Entities")
                .description("Used to get all fake entities viewed by a player")
                .examples(
                        """
                        command /private-preview:
                            trigger:
                                set {_e} to a new fake zombie entity:
                                    location: location of player
                                    viewers: players

                                if visible fake entities of player contains {_e}:
                                    send "You can see the fake entity!"
                        """)
                .since("1.0.0")
                .register();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(
            Expression<?>[] expressions,
            int matchedPattern,
            Kleenean isDelayed,
            SkriptParser.ParseResult parseResult) {
        setExpr((Expression<? extends Player>) expressions[0]);
        return true;
    }

    @Override
    protected WrapperEntity[] get(Event event, Player[] source) {
        Set<UUID> playerUUIDs =
                Arrays.stream(source).map(Player::getUniqueId).collect(Collectors.toSet());

        return EntityLib.get().getEntities().stream()
                .filter(entity -> entity.getViewers().stream().anyMatch(playerUUIDs::contains))
                .toArray(WrapperEntity[]::new);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends WrapperEntity> getReturnType() {
        return WrapperEntity.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "visible fake entities of " + getExpr().toString(event, debug);
    }
}
