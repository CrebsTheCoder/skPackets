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
package org.crebs.skpackets.elements.entity.expressions.entity;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.entity.expressions.prop.ExprFakeEntityField;

public final class ExprFakeEntityPlayerSkin extends ExprFakeEntityField {
    public ExprFakeEntityPlayerSkin() {
        super("player skin");
    }

    public static void register(Registration registration) {
        if (!isAvailable("player skin")) {
            return;
        }
        registration
                .newPropertyExpression(
                        ExprFakeEntityPlayerSkin.class,
                        Object.class,
                        "(fake|packet) entity [field] [player] skin",
                        "packetentity")
                .name("Fake Entity - Player Skin")
                .description(
                        "The fake player skin, including texture value and signature. Value type:"
                            + " Skin. Applies to player and supported subclasses. Supports set.")
                .examples(
                        """
                        command /my-double:
                            trigger:
                                set {_entity} to a new fake player entity:
                                    location: location of player
                                    viewers: player
                                    username: "Demo"
                                    skin: skin of player
                                set fake entity player skin of {_entity} to skin of player
                                wait 5 seconds
                                kill fake entity {_entity}
                        """)
                .since("1.0.0")
                .register();
    }
}
