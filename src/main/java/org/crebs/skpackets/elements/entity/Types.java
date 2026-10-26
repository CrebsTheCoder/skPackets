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
package org.crebs.skpackets.elements.entity;

import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;

import com.github.shanebeee.skr.Registration;

import me.tofaa.entitylib.wrapper.WrapperEntity;

import org.crebs.skpackets.api.entity.metadata.EntityMeta;
import org.skriptlang.skript.lang.converter.Converters;

public class Types {

    public static void register(Registration reg) {
        Converters.registerConverter(WrapperEntity.class, EntityMeta.class, EntityMeta::of);

        reg.newType(EntityMeta.class, "entitymeta")
                .user("(fake|packet) ?entit(y|ies) meta")
                .name("General - Entity Meta")
                .description(
                        "The entity meta of a minecraft entity (this can both represent a fake"
                            + " entity's meta or a real entity's meta, but is mostly used for fake"
                            + " entities since the only use for real entities is for packet"
                            + " intercepting).")
                .examples(
                        """
                        command /types:
                            trigger:
                                set {_zombie} to a new fake zombie entity:
                                    viewers: all players
                                    location: location of player

                                set {_meta} to fake entity meta of {_zombie}
                                set meta glowing state of {_meta} to true
                        """)
                .since("1.0.0")
                .parser(
                        new Parser<>() {
                            @Override
                            public boolean canParse(ParseContext context) {
                                return false;
                            }

                            @Override
                            public String toString(EntityMeta meta, int flags) {
                                return "entity meta";
                            }

                            @Override
                            public String toVariableNameString(EntityMeta meta) {
                                return "entitymeta:" + meta.hashCode();
                            }
                        })
                .register();
    }
}
