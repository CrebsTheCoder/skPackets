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
package org.crebs.skpackets.elements.general.expressions.prop;

import ch.njol.skript.expressions.base.SimplePropertyExpression;

import com.github.shanebeee.skr.Registration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.crebs.skpackets.api.entity.Skin;
import org.crebs.skpackets.api.general.PlayerSkinRegistry;
import org.jetbrains.annotations.Nullable;

public class ExprPlayerSkin extends SimplePropertyExpression<Player, Skin> {

    private static final boolean ONLINE_MODE =
            Bukkit.getServer().getServerConfig().isProxyOnlineMode();

    public static void register(Registration reg) {
        if (ONLINE_MODE) {
            reg.newPropertyExpression(ExprPlayerSkin.class, Skin.class, "skin", "player")
                    .name("General - Player Skin Of Player")
                    .description(
                            """
                            Gets the skin the player had when they joined the server
                            NOTE: This only works on servers in online mode
                            """)
                    .examples(
                            """
                            command revertToMySkin:
                                trigger:
                                    set displayed skin of player to player's skin
                            """)
                    .since("1.0.0")
                    .register();
        }
    }

    @Override
    public @Nullable Skin convert(Player player) {
        if (!ONLINE_MODE) {
            return null;
        }
        return PlayerSkinRegistry.getSkin(player.getUniqueId());
    }

    @Override
    public Class<? extends Skin> getReturnType() {
        return Skin.class;
    }

    @Override
    protected String getPropertyName() {
        return "skin";
    }
}
