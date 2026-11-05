package org.crebs.skpackets.api.util.field;

import ch.njol.skript.Skript;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FieldValuesTest {
    @Test
    void invalidArrayValuesReportAnErrorAndPreserveTheField() {
        Object source = new Object();
        Number[] original = {1, 2};
        AtomicReference<Number[]> stored = new AtomicReference<>(original);
        FieldAccessor<Object, Number[]> accessor = new FieldAccessor<>(
                "numbers", new String[0], Number[].class, false,
                ignored -> stored.get(), (ignored, value) -> stored.set(value));
        try (var skript = mockStatic(Skript.class)) {
            assertDoesNotThrow(() -> FieldValues.set(new Object[]{source}, ignored -> accessor,
                    new Object[]{"invalid"}, "packet"));
            assertSame(original, stored.get());
            skript.verify(() -> Skript.error(anyString()));
            FieldValues.set(new Object[]{source}, ignored -> accessor, new Object[]{3, 4}, "packet");
            assertArrayEquals(new Number[]{3, 4}, stored.get());
        }
    }
}
