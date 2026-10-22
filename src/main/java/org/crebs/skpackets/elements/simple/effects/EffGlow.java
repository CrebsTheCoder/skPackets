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
package org.crebs.skpackets.elements.simple.effects;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;

import com.github.shanebeee.skr.Registration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.simple.GlowingEntityManager;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class EffGlow extends Effect {

    public static void register(Registration reg) {
        reg.newEffect(
                        EffGlow.class,
                        "set glow[ing] [state] of %entities% [to] %boolean% [for %-players%]")
                .name("Simple Glow - Entity Glow State")
                .description(
                        """
                        Set the glow state of an entity for a set of viewers.
                        Internally everything is handled by the addon. (fully packet based)
                        """)
                .examples(
                        """
                        command glowMeForMe:
                            trigger:
                                set glow state of player to true for player
                        """)
                .since("1.0.0")
                .register();
    }

    private Expression<Entity> targetExpr;
    private Expression<Boolean> stateExpr;
    private @Nullable Expression<Player> viewersExpr;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(
            Expression<?>[] expressions,
            int matchedPattern,
            Kleenean isDelayed,
            SkriptParser.ParseResult parseResult) {
        this.targetExpr = (Expression<Entity>) expressions[0];
        this.stateExpr = (Expression<Boolean>) expressions[1];
        this.viewersExpr = (Expression<Player>) expressions[2];

        return true;
    }

    @Override
    protected void execute(Event event) {
        Entity[] targets = targetExpr.getAll(event);
        Boolean newState = stateExpr.getSingle(event);

        if (targets == null || targets.length == 0 || newState == null) {
            return;
        }

        Player[] viewers = viewersExpr != null ? viewersExpr.getAll(event) : null;

        Set<UUID> viewerUuids;

        if (viewers == null || viewers.length == 0) {
            viewerUuids =
                    Bukkit.getOnlinePlayers().stream()
                            .map(Player::getUniqueId)
                            .collect(Collectors.toSet());
        } else {
            viewerUuids =
                    Arrays.stream(viewers).map(Player::getUniqueId).collect(Collectors.toSet());
        }

        for (Entity target : targets) {
            int entityId = target.getEntityId();

            if (newState) {
                GlowingEntityManager.addGlowingReceivers(entityId, viewerUuids);
            } else {
                GlowingEntityManager.removeGlowingReceivers(entityId, viewerUuids);
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String target = targetExpr.toString(event, debug);
        String glowingState = stateExpr.toString(event, debug);
        String viewersPart =
                viewersExpr != null ? " for " + viewersExpr.toString(event, debug) : "";
        return String.format("set glowing state of %s to %s%s", target, glowingState, viewersPart);
    }
}
