package xyz.geik.gmanager.modules.events;

import lombok.Getter;
import lombok.Setter;
import xyz.geik.gmanager.api.handlers.GManagerEvent;
import xyz.geik.gmanager.modules.Module;

import java.util.Map;
import java.util.Optional;

/**
 * Base abstract class for addon events
 * @author Poslovitch
 */
public abstract class ModuleBaseEvent extends GManagerEvent {

    @Getter
    private final Module module;
    private final Map<String, Object> keyValues;
    /**
     * -- SETTER --
     *  Set the newer event so it can be obtained if this event is deprecated
     *
     * @param newEvent the newEvent to set
     */
    @Setter
    private ModuleBaseEvent newEvent;

    protected ModuleBaseEvent(Module module, Map<String, Object> keyValues) {
        super();
        this.module = module;
        this.keyValues = keyValues;
    }

    /**
     * @return the keyValues
     */
    @Override
    public Map<String, Object> getKeyValues() {
        return keyValues;
    }

    /**
     * Get new event if this event is deprecated
     * @return optional newEvent or empty if there is none
     */
    public Optional<ModuleBaseEvent> getNewEvent() {
        return Optional.ofNullable(newEvent);
    }
}