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

public final class ExprEntityMetaDisplayItem extends ExprMetaField {
    public ExprEntityMetaDisplayItem() {
        super("display item");
    }

    public static void register(Registration registration) {
        if (!isAvailable("display item")) {
            return;
        }
        registration
                .newPropertyExpression(
                        ExprEntityMetaDisplayItem.class,
                        Object.class,
                        "[(fake|packet)] [entity] meta [field] (display item stack|display item)",
                        "entitymeta")
                .name("Entity Meta - Display Item")
                .description(
                        "The display item property. Value type: ItemType. Applies to item display"
                            + " and supported subclasses. Supports set. When editing metadata"
                            + " copied from a packet, set packet meta back to the edited object.")
                .examples(
                        """
                        command /floating-diamond:
                            trigger:
                                set {_mob} to a new fake item display entity:
                                    location: location of player
                                    viewers: player
                                set {_meta} to fake entity meta data of {_mob}
                                set meta display item of {_meta} to diamond
                                wait 5 seconds
                                kill fake entity {_mob}
                        """)
                .since("1.0.0")
                .register();
    }
}
