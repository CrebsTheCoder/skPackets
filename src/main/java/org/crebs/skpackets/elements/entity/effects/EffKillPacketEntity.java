package org.crebs.skpackets.elements.entity.effects;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.github.shanebeee.skr.Registration;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.event.Event;

public class EffKillPacketEntity extends Effect {

    public static void register(Registration registration) {
        registration.newEffect(EffKillPacketEntity.class,
                "kill packet entity %packetentity%")
                .name("Kill Packet Entity")
                .description("Kills the packet entity")
                .examples("kill packet entity {_entity}")
                .since("1.0.0")
                .register();
    }

    private Expression<WrapperEntity> entityInput;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions,
                        int matchedPattern,
                        Kleenean isDelayed,
                        ParseResult parseResult) {
        entityInput = (Expression<WrapperEntity>) expressions[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        WrapperEntity entity = entityInput.getSingle(event);

        if (entity == null) {
            return;
        }

        entity.remove();
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "remove packet entity " + entityInput.toString(event, debug);
    }
}