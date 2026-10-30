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
package org.crebs.skpackets.elements.general;

import com.github.shanebeee.skr.Registration;
import org.crebs.skpackets.elements.RegistrationModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.crebs.skpackets.elements.general.condition.CondPacketTypeIsBound;
import org.crebs.skpackets.elements.general.effects.EffCancelPacket;
import org.crebs.skpackets.elements.general.effects.EffFetchSkin;
import org.crebs.skpackets.elements.general.effects.EffSendOrReceivePacket;
import org.crebs.skpackets.elements.general.event.EvtNettyPacket;
import org.crebs.skpackets.elements.general.event.EvtPacketSendOrReceive;
import org.crebs.skpackets.elements.general.expressions.ExprSkinFromValue;
import org.crebs.skpackets.elements.general.expressions.prop.ExprEntityId;
import org.crebs.skpackets.elements.general.expressions.prop.ExprPacketField;
import org.crebs.skpackets.elements.general.expressions.prop.ExprPacketPacketType;
import org.crebs.skpackets.elements.general.expressions.prop.ExprPlayerSkin;
import org.crebs.skpackets.elements.general.field.PacketFieldRegistry;
import org.crebs.skpackets.elements.general.section.SecExprNewPacket;

public record GeneralModule(Registration registration) implements RegistrationModule {

    @Override
    public String name() {
        return "general";
    }

    @Override
    public void load(SkriptAddon addon) {
        PacketFieldRegistry.INSTANCE.registerAll();
        register(registration,
                Types::register,
                CondPacketTypeIsBound::register,
                EffCancelPacket::register,
                EffFetchSkin::register,
                EffSendOrReceivePacket::register,
                EvtPacketSendOrReceive::register,
                EvtNettyPacket::register,
                SecExprNewPacket::register,
                ExprEntityId::register,
                ExprPacketField::register,
                ExprPacketPacketType::register,
                ExprPlayerSkin::register,
                ExprSkinFromValue::register);
    }
}
