package org.crebs.skpackets.api.util;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DebugUtilTest {
    @BeforeAll static void initialize() { PacketEventsTestBootstrap.initialize(); }

    @Test void formatsTypedMetadataIncludingNullAndMixedEntries() {
        Holder holder = new Holder(Arrays.asList(new EntityData<>(2, EntityDataTypes.STRING, "hello"), null, "other"));
        String output = DebugUtil.getDebugString(holder);
        assertTrue(output.contains("[2:"), output);
        assertTrue(output.contains(" = hello]"), output);
        assertTrue(output.contains("<none> metadata entry"), output);
        assertTrue(output.contains("Unparsable Data: other"), output);
        assertFalse(output.contains("Reflection Error"), output);
    }

    @Test void repeatedDebugReadsStillUseCurrentValues() {
        Holder holder = new Holder(List.of("first"));
        assertTrue(DebugUtil.getDebugString(holder).contains("first"));
        holder.entries = List.of("second");
        assertTrue(DebugUtil.getDebugString(holder).contains("second"));
    }

    public static class Holder {
        private List<?> entries;
        Holder(List<?> entries) { this.entries = entries; }
        public List<?> getEntries() { return entries; }
    }
}
