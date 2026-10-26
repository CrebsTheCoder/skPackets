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
package org.crebs.skpackets.api.entity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.MojangAPIUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class SkinManager {

    private static final Cache<String, Skin> skinCache =
            CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(5)).build();

    public static Skin getPlayer(final Player player) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        List<TextureProperty> properties = user.getProfile().getTextureProperties();

        return new Skin(properties);
    }

    public static Skin getOfflinePlayer(final String playerName) {

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(playerName)) {
                return getPlayer(player);
            }
        }

        Skin skin = skinCache.getIfPresent(playerName);
        if (skin != null) {
            return skin;
        }

        UUID uuid = MojangAPIUtil.requestPlayerUUID(playerName);
        List<TextureProperty> properties = MojangAPIUtil.requestPlayerTextureProperties(uuid);

        skin = new Skin(properties);
        skinCache.put(playerName, skin);

        return skin;
    }
}
