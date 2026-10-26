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

public final class ExprEntityMetaDisplayUseDefaultBackgroundState extends ExprMetaField {
    public ExprEntityMetaDisplayUseDefaultBackgroundState() {
        super("display use default background state");
    }

    public static void register(Registration registration) {
        if (!isAvailable("display use default background state")) {
            return;
        }
        registration
                .newPropertyExpression(
                        ExprEntityMetaDisplayUseDefaultBackgroundState.class,
                        Object.class,
                        "[(fake|packet)] [entity] meta [field] display use default background [state]",
                        "entitymeta")
                .name("Entity Meta - Display Use Default Background State")
                .description(
                        "The display use default background state property. Value type: Boolean."
                            + " Applies to text display and supported subclasses. Supports set."
                            + " When editing metadata copied from a packet, set packet meta back to"
                            + " the edited object.")
                .examples(
                        """
                        command /hologram-use-default-background-state:
                            trigger:
                                set {_displayMeta} to text display meta data:
                                    display content: "Hello!"
                                set {_mob} to a new fake text display entity:
                                    location: location of player
                                    viewers: player
                                    meta: {_displayMeta}
                                set {_meta} to fake entity meta data of {_mob}
                                set meta display use default background state of {_meta} to true
                                wait 5 seconds
                                kill fake entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
