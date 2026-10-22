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

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.entity.Skin;
import org.crebs.skpackets.api.simple.PlayerSkinManager;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class EffDisplayedSkin extends Effect {

    public static void register(Registration reg) {
        reg.newEffect(
                        EffDisplayedSkin.class,
                        "set displayed skin of %players% to %skin% [for %-players%]")
                .name("Simple Skin - Player Displayed Skin")
                .description(
                        """
                        Set the displayed skin of a player for a set of viewers.
                        Internally everything is handled by the addon (fully packet based).
                        """)
                .examples(
                        """
                        command skinMeNotchForMe:
                            trigger:
                                fetch skin of player named "notch" and store it in {_skin}
                                set displayed skin of player to {_skin} for player
                        """)
                .since("1.0.0")
                .register();
    }

    private Expression<Player> targetExpr;
    private Expression<Skin> skinExpr;
    private @Nullable Expression<Player> viewersExpr;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(
            Expression<?>[] expressions,
            int matchedPattern,
            Kleenean isDelayed,
            SkriptParser.ParseResult parseResult) {
        this.targetExpr = (Expression<Player>) expressions[0];
        this.skinExpr = (Expression<Skin>) expressions[1];
        this.viewersExpr = (Expression<Player>) expressions[2];

        return true;
    }

    @Override
    protected void execute(Event event) {
        Player[] targets = targetExpr.getAll(event);
        Skin newSkin = skinExpr.getSingle(event);

        if (targets == null || targets.length == 0 || newSkin == null) {
            return;
        }

        Player[] viewers = viewersExpr != null ? viewersExpr.getAll(event) : new Player[0];

        if (viewers == null || viewers.length == 0) {
            for (Player target : targets) {
                PlayerSkinManager.setGlobalSkin(target, newSkin);
            }
            return;
        }

        for (Player target : targets) {
            PlayerSkinManager.setSkinForViewers(target, Arrays.stream(viewers).toList(), newSkin);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String target = targetExpr.toString(event, debug);
        String skin = skinExpr.toString(event, debug);
        String viewersPart =
                viewersExpr != null ? " for " + viewersExpr.toString(event, debug) : "";
        return String.format("set displayed skin of %s to %s%s", target, skin, viewersPart);
    }
}
