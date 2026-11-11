package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.CollisionRule;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.NameTagVisibility;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.crebs.skpackets.api.util.field.FieldAccessor;
import org.crebs.skpackets.elements.team.TeamField;
import org.crebs.skpackets.elements.team.expressions.ExprTeamCollisionRule;
import org.crebs.skpackets.elements.team.expressions.ExprTeamNameTagVisibility;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TypedTeamFieldsTest {
    @BeforeAll static void initialize() {
        PacketEventsTestBootstrap.initialize();
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            PacketFieldRegistry.INSTANCE.registerAll();
        }
    }

    @ParameterizedTest @EnumSource(CollisionRule.class)
    void packetBuilderCollisionRuleUsesText(CollisionRule rule) {
        var packet = mock(WrapperPlayServerTeams.class);
        var info = mock(WrapperPlayServerTeams.ScoreBoardTeamInfo.class);
        when(packet.getTeamInfo()).thenReturn(Optional.of(info));
        when(info.getCollisionRule()).thenReturn(rule);
        var field = field("collision rule");
        String text = rule.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        field.setter().accept(packet, text);
        verify(info).setCollisionRule(rule);
        verify(packet).setTeamInfo(info);
        assertSame(String.class, field.expectedType());
        assertEquals(text, field.getter().apply(packet));
        assertEquals("packet team collision rule", PacketValueTypes.label(CollisionRule.class));
    }

    @ParameterizedTest @EnumSource(NameTagVisibility.class)
    void packetBuilderNameTagVisibilityUsesText(NameTagVisibility visibility) {
        var packet = mock(WrapperPlayServerTeams.class);
        var info = mock(WrapperPlayServerTeams.ScoreBoardTeamInfo.class);
        when(packet.getTeamInfo()).thenReturn(Optional.of(info));
        when(info.getTagVisibility()).thenReturn(visibility);
        var field = field("name tag visibility");
        String text = visibility.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        field.setter().accept(packet, text);
        verify(info).setTagVisibility(visibility);
        verify(packet).setTeamInfo(info);
        assertSame(String.class, field.expectedType());
        assertEquals(text, field.getter().apply(packet));
    }

    @Test void packetBuilderRejectsUnknownTextValues() {
        var packet = mock(WrapperPlayServerTeams.class);
        when(packet.getTeamInfo()).thenReturn(Optional.of(mock(WrapperPlayServerTeams.ScoreBoardTeamInfo.class)));
        assertThrows(IllegalArgumentException.class, () -> field("collision rule").setter().accept(packet, "invalid collision rule"));
        assertThrows(IllegalArgumentException.class, () -> field("name tag visibility").setter().accept(packet, "invalid visibility"));
    }

    @ParameterizedTest @EnumSource(CollisionRule.class)
    void oldTeamCollisionExpressionStillAcceptsAndReturnsText(CollisionRule rule) {
        var packet = mock(WrapperPlayServerTeams.class);
        var info = mock(WrapperPlayServerTeams.ScoreBoardTeamInfo.class);
        when(packet.getTeamInfo()).thenReturn(Optional.of(info));
        when(info.getCollisionRule()).thenReturn(rule);
        String text = rule.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        TeamField.COLLISION_RULE.set(packet, text);
        verify(info).setCollisionRule(rule);
        assertEquals(text, TeamField.COLLISION_RULE.get(packet));
        assertSame(String.class, new ExprTeamCollisionRule().getReturnType());
    }

    @ParameterizedTest @EnumSource(NameTagVisibility.class)
    void oldTeamVisibilityExpressionStillAcceptsAndReturnsText(NameTagVisibility visibility) {
        var packet = mock(WrapperPlayServerTeams.class);
        var info = mock(WrapperPlayServerTeams.ScoreBoardTeamInfo.class);
        when(packet.getTeamInfo()).thenReturn(Optional.of(info));
        when(info.getTagVisibility()).thenReturn(visibility);
        String text = visibility.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        TeamField.NAME_TAG_VISIBILITY.set(packet, text);
        verify(info).setTagVisibility(visibility);
        assertEquals(text, TeamField.NAME_TAG_VISIBILITY.get(packet));
        assertSame(String.class, new ExprTeamNameTagVisibility().getReturnType());
    }

    @SuppressWarnings("unchecked")
    private static FieldAccessor<PacketWrapper<?>, Object> field(String name) {
        return (FieldAccessor<PacketWrapper<?>, Object>) PacketFieldRegistry.INSTANCE.getSchema(PacketType.Play.Server.TEAMS).getAccessor(name);
    }
}
