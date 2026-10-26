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

public final class ExprFakeEntityEntityId extends ExprFakeEntityField {
    public ExprFakeEntityEntityId() {
        super("entity id");
    }

    public static void register(Registration registration) {
        if (!isAvailable("entity id")) {
            return;
        }
        registration
                .newPropertyExpression(
                        ExprFakeEntityEntityId.class,
                        Object.class,
                        "(fake|packet) entity [field] [entity] id",
                        "packetentity")
                .name("Fake Entity - Entity ID")
                .description(
                        "The numeric network entity ID. Read-only. Value type: Number. Applies to"
                                + " entity and supported subclasses. Read-only.")
                .examples(
                        """
                        command /who-am-i:
                            trigger:
                                set {_entity} to a new fake zombie entity:
                                    location: location of player
                                    viewers: player
                                set {_value} to fake entity entity id of {_entity}
                                send "%{_value}%" to player
                                wait 5 seconds
                                kill fake entity {_entity}
                        """)
                .since("1.0.0")
                .register();
    }
}
