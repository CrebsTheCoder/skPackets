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
package org.crebs.skpackets.elements.entity.field.entity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.UserProfile;

import me.tofaa.entitylib.wrapper.WrapperPlayer;

import org.crebs.skpackets.api.entity.Skin;
import org.crebs.skpackets.api.util.field.FieldRegistrar;

public class FakePlayerEntityFieldRegistrar implements FieldRegistrar {

    @Override
    public void register() {

        FakeEntityFieldRegistry.INSTANCE
                .builder(EntityTypes.PLAYER, WrapperPlayer.class)
                .requiredField(
                        Skin.class,
                        w -> new Skin(w.getTextures()),
                        (w, skin) -> w.setTextureProperties(skin.properties()),
                        "player skin",
                        "skin")
                .requiredField(
                        String.class,
                        WrapperPlayer::getUsername,
                        null,
                        "player username",
                        "player name",
                        "username",
                        "name")
                .constructor(
                        context -> {
                            FakeBaseEntityFieldRegistrar.CommonEntityData data =
                                    FakeBaseEntityFieldRegistrar.CommonEntityData.extract(context);

                            String name = context.getRequired("player username", String.class);
                            if (!name.matches("[A-Za-z0-9_]{1,16}")) {
                                throw new IllegalArgumentException(
                                        "Player usernames must contain 1 to 16 letters, numbers or"
                                                + " underscores.");
                            }
                            UserProfile profile = new UserProfile(data.uuid(), name);

                            WrapperPlayer player = new WrapperPlayer(profile, data.entityId());

                            if (data.meta() != null) {
                                data.meta().applyTo(player);
                            }

                            Skin skin = context.getRequired("player skin", Skin.class);
                            player.setTextureProperties(skin.properties());

                            FakeBaseEntityFieldRegistrar.finalizeEntitySetup(player, context);

                            return player;
                        })
                .build();
    }
}
