package org.crebs.skpackets.elements;

import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptEvent;

import com.github.shanebeee.skr.Registration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.skriptlang.skript.lang.structure.Structure;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ElementCounts {
    private ElementCounts() {}

    public static void log(Registration registration, CommandSender console) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("events", 0);
        counts.put("effects", 0);
        counts.put("expressions", 0);
        counts.put("conditions", 0);
        counts.put("sections", 0);
        counts.put("structures", 0);

        for (var syntax : registration.getAddon().syntaxRegistry().elements()) {
            if (!syntax.origin().name().equals(registration.getAddon().name())) {
                continue;
            }
            String category = category(syntax.type());
            if (category != null) {
                counts.computeIfPresent(category, (key, count) -> count + 1);
            }
        }

        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        Component prefix =
                Component.text("[" + registration.getAddon().name() + "] ", NamedTextColor.AQUA);
        console.sendMessage(
                prefix.append(Component.text("Loaded ", NamedTextColor.GRAY))
                        .append(Component.text(total, NamedTextColor.GREEN))
                        .append(Component.text(" elements:", NamedTextColor.GRAY)));
        counts.forEach(
                (category, count) -> {
                    if (count > 0) {
                        console.sendMessage(
                                prefix.append(Component.text(" - ", NamedTextColor.GRAY))
                                        .append(Component.text(count, NamedTextColor.GREEN))
                                        .append(
                                                Component.text(
                                                        " " + category, NamedTextColor.GRAY)));
                    }
                });
    }

    private static String category(Class<?> type) {
        if (SkriptEvent.class.isAssignableFrom(type)) {
            return "events";
        }
        if (Section.class.isAssignableFrom(type)
                || SectionExpression.class.isAssignableFrom(type)) {
            return "sections";
        }
        if (Effect.class.isAssignableFrom(type)) {
            return "effects";
        }
        if (Condition.class.isAssignableFrom(type)) {
            return "conditions";
        }
        if (Expression.class.isAssignableFrom(type)) {
            return "expressions";
        }
        if (Structure.class.isAssignableFrom(type)) {
            return "structures";
        }
        return null;
    }
}
