package xyz.geik.gmanager.modules.events;

import java.util.Map;

import org.bukkit.event.HandlerList;
import xyz.geik.gmanager.modules.Module;

/**
 * General event that can be called for Addons
 * @author tastybento
 *
 */
public class ModuleGeneralEvent extends ModuleBaseEvent {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    ModuleGeneralEvent(Module module, Map<String, Object> keyValues) {
        // Final variables have to be declared in the constructor
        super(module, keyValues);
    }
}