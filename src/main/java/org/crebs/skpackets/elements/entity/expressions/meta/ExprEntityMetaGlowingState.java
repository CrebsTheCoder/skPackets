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
package org.crebs.skpackets.elements.entity.expressions.meta;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.elements.entity.expressions.prop.ExprMetaField;

public final class ExprEntityMetaGlowingState extends ExprMetaField {
    public ExprEntityMetaGlowingState() {
        super("glowing state");
    }

    public static void register(Registration registration) {
        if (!isAvailable("glowing state")) {
            return;
        }
        registration
                .newPropertyExpression(
                        ExprEntityMetaGlowingState.class,
                        Object.class,
                        "[(fake|packet)] [entity] meta [field] glowing [state]",
                        "entitymeta")
                .name("Entity Meta - Glowing State")
                .description(
                        "Enables the entity outline. A packet team supplies its outline color."
                            + " Value type: Boolean. Applies to entity and supported subclasses."
                            + " Supports set. When editing metadata copied from a packet, set"
                            + " packet meta back to the edited object.")
                .examples(
                        """
                        command /spotlight:
                            trigger:
                                set {_mob} to a new fake zombie entity:
                                    location: location of player
                                    viewers: player
                                set {_meta} to fake entity meta data of {_mob}
                                set meta glowing state of {_meta} to true
                                wait 5 seconds
                                kill fake entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
