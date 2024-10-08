package xyz.geik.gmanager.api.handlers;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import xyz.geik.gmanager.GManager;

/**
 * Provides the default methods expected when extending {@link Event}.
 * @author tastybento
 * @since 1.5.3
 *
 */
public abstract class GManagerEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    /**
     * This is here just for backwards compatibility. Users of GManagerEvent should implement their own getHandlers
     */
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    /**
     * This is here just for backwards compatibility. Users of GManagerEvent should implement their own getHandlers
     *
     * @return HandlerList
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * The default constructor is defined for cleaner code.
     * This constructor assumes the GManagerEvent is synchronous.
     */
    protected GManagerEvent() {
        this(false);
    }

    /**
     * Explicitly declares a GManagerEvent as synchronous or asynchronous.
     *
     * @param async - true indicates the event will fire asynchronously, false
     *              by default from default constructor
     */
    protected GManagerEvent(boolean async) {
        super(async);
    }

    /**
     * Get a map of key value pairs derived from the fields of this class by reflection.
     *
     * @return map
     * @since 1.5.3
     */
    public Map<String, Object> getKeyValues() {
        try {
            Map<String, Object> map = new HashMap<>();
            Arrays.stream(Introspector.getBeanInfo(this.getClass(), GManagerEvent.class).getPropertyDescriptors())
                    // only get getters
                    .filter(pd -> Objects.nonNull(pd.getReadMethod()))
                    .forEach(pd -> { // invoke method to get value
                        try {
                            Object value = pd.getReadMethod().invoke(this);
                            if (value != null) {
                                map.put(pd.getName(), value);
                            }
                        } catch (Exception ignore) {
                            // Ignored.
                        }
                    });
            return map;
        } catch (IntrospectionException e) {
            // Oh well, nothing
            return Collections.emptyMap();
        }
    }

    /**
     * Set values back to the event. Use {@link #getKeyValues()} to obtain the map
     *
     * @param map - key value map (Name of key, value - object)
     * @since 1.15.1
     */
    public void setKeyValues(Map<String, Object> map) {
        try {
            Arrays.stream(Introspector.getBeanInfo(this.getClass(), GManagerEvent.class).getPropertyDescriptors())
                    // only get setters
                    .filter(pd -> Objects.nonNull(pd.getWriteMethod()))
                    .forEach(pd -> { // invoke method to set value
                        if (map.containsKey(pd.getName())) {
                            try {
                                pd.getWriteMethod().invoke(this, map.get(pd.getName()));
                            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                                GManager.getInstance().logStacktrace(e);
                            }
                        }
                    });
        } catch (IntrospectionException ignore) {
            // Ignored.
        }
    }
}
