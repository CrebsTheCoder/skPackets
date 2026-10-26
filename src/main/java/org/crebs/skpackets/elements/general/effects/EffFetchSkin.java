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
package org.crebs.skpackets.elements.general.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.AsyncEffect;
import ch.njol.util.Kleenean;

import com.github.shanebeee.skr.Registration;

import org.bukkit.event.Event;
import org.crebs.skpackets.api.entity.Skin;
import org.crebs.skpackets.api.entity.SkinManager;
import org.jetbrains.annotations.Nullable;

public class EffFetchSkin extends AsyncEffect {

    public static void register(Registration reg) {
        reg.newEffect(
                        EffFetchSkin.class,
                        "fetch skin (from|of) player (named|with name) %string% and store (it|the"
                                + " result) in %-~objects%",
                        "set %-~objects% to [fetch] skin (from|of) player (named|with name)"
                                + " %string%")
                .name("General - Fetch Skin")
                .description(
                        """
                        Used to fetch a player skin.
                        **This is processed on an async thread and thus will pause your code until it's been loaded.**
                        """)
                .examples(
                        """
                        command changeMySkinForMe <text>:
                            trigger:
                                fetch skin of player named arg-1 and store it in {_skin}
                                if {_skin} isn't set:
                                    send "Couldn't find a skin!"
                                    stop

                                set displayed skin of player to {_skin} for player
                        """)
                .since("1.0.0")
                .register();
    }

    private Expression<String> nameExpr;
    private Expression<?> expression;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(
            Expression<?>[] expressions,
            int matchedPattern,
            Kleenean isDelayed,
            SkriptParser.ParseResult parseResult) {
        getParser().setHasDelayBefore(Kleenean.TRUE);

        if (matchedPattern == 0) {
            this.nameExpr = (Expression<String>) expressions[0];
            this.expression = expressions[1];

        } else {
            this.expression = expressions[0];
            this.nameExpr = (Expression<String>) expressions[1];
        }

        if (!Changer.ChangerUtils.acceptsChange(
                this.expression, Changer.ChangeMode.SET, Skin.class)) {
            Skript.error(
                    this.expression.toString(null, Skript.debug()) + " cannot be set to a skin.");
            return false;
        }

        return true;
    }

    @Override
    protected void execute(Event event) {
        String name = this.nameExpr.getSingle(event);

        Skin skin;
        try {
            skin = SkinManager.getOfflinePlayer(name);
        } catch (IllegalStateException error) {
            return;
        }

        this.expression.change(event, new Skin[] {skin}, Changer.ChangeMode.SET);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String name = this.nameExpr.getSingle(event);
        String store = this.expression.toString(event, debug);
        return String.format("skin from player named %s and store it in %s", name, store);
    }
}
