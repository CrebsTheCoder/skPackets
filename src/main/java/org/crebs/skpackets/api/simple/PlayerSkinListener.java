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
package org.crebs.skpackets.api.simple;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.PlayerInfo;
import org.crebs.skpackets.api.entity.Skin;

import static com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER;

public class PlayerSkinListener implements PacketListener {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!event.getPacketType().equals(PacketType.Play.Server.PLAYER_INFO_UPDATE)) {
            return;
        }

        User viewer = event.getUser();

        WrapperPlayServerPlayerInfoUpdate packet = new WrapperPlayServerPlayerInfoUpdate(event);
        if (!packet.getActions().contains(ADD_PLAYER)) {
            return;
        }

        var entries = new java.util.ArrayList<PlayerInfo>(packet.getEntries());
        boolean changed = false;
        for (int i = 0; i < entries.size(); i++) {
            PlayerInfo entry = entries.get(i);
            UserProfile profile = entry.getGameProfile();
            Skin skin =
                    PlayerSkinManager.getPersistentSkinForViewer(
                            profile.getUUID(), viewer.getUUID());
            if (skin == null) continue;
            PlayerInfo replacement = new PlayerInfo(entry);
            replacement.setGameProfile(
                    new UserProfile(profile.getUUID(), profile.getName(), skin.properties()));
            entries.set(i, replacement);
            changed = true;
        }
        if (changed) {
            packet.setEntries(entries);
            event.markForReEncode(true);
        }
    }
}
