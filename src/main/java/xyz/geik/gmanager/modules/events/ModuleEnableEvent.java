package xyz.geik.gmanager.modules.events;

import java.util.Map;

import org.bukkit.event.HandlerList;
import xyz.geik.gmanager.modules.Module;

/**
 * Called when an addon is enabled
 * @author tastybento
 *
 */
public class ModuleEnableEvent extends ModuleBaseEvent {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    ModuleEnableEvent(Module module, Map<String, Object> keyValues) {
        // Final variables have to be declared in the constructor
        super(module, keyValues);
    }

}