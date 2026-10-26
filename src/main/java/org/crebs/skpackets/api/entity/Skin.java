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

import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Objects;

public record Skin(List<TextureProperty> properties) {
    public static final String TEXTURE_KEY = "textures";

    public Skin(final String value, final String signature) {
        this(List.of(new TextureProperty(TEXTURE_KEY, value, signature)));
    }

    public Skin {
        properties = List.copyOf(properties);
        Preconditions.checkArgument(!properties.isEmpty());
        Preconditions.checkArgument(properties.getFirst().getName().equals(TEXTURE_KEY));
    }

    @Override
    public int hashCode() {
        TextureProperty texture = properties.getFirst();
        return Objects.hash(texture.getValue(), texture.getSignature());
    }

    @Override
    public boolean equals(final Object object) {

        if (object == this) {
            return true;
        } else if (object instanceof Skin(List<TextureProperty> properties1)) {

            final boolean sign =
                    Objects.equals(
                            this.properties.getFirst().getSignature(),
                            properties1.getFirst().getSignature());
            final boolean value =
                    Objects.equals(
                            this.properties.getFirst().getValue(),
                            properties1.getFirst().getValue());

            return sign && value;
        }

        return false;
    }
}
