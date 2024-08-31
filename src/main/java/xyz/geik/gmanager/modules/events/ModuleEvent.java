package xyz.geik.gmanager.modules.events;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import xyz.geik.gmanager.modules.Module;

public class ModuleEvent {

    public enum Reason {
        ENABLE,
        DISABLE,
        LOAD,
        UNKNOWN
    }

    /**
     * @return Addon event builder
     */
    public AddonEventBuilder builder() {
        return new AddonEventBuilder();
    }

    public class AddonEventBuilder {
        // Here field are NOT final. They are just used for the building.
        private Module module;
        private Reason reason = Reason.UNKNOWN;
        private Map<String, Object> keyValues = new HashMap<>();

        /**
         * Add a map of key-value pairs to the event. Use this to transfer data from the addon to the external world.
         * @param keyValues - map
         * @return AddonEvent
         */
        public AddonEventBuilder keyValues(Map<String, Object> keyValues) {
            this.keyValues = keyValues;
            return this;
        }

        public AddonEventBuilder addon(Module module) {
            this.module = module;
            return this;
        }

        public AddonEventBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        private ModuleBaseEvent getEvent() {
            return switch (reason) {
                case ENABLE -> new ModuleEnableEvent(module, keyValues);
                case DISABLE -> new ModuleDisableEvent(module, keyValues);
                case LOAD -> new ModuleLoadEvent(module, keyValues);
                default -> new ModuleGeneralEvent(module, keyValues);
            };
        }

        /**
         * Build and fire event
         * @return event - deprecated event. To obtain the new event use {@link ModuleBaseEvent#getNewEvent()}
         */
        public ModuleBaseEvent build() {
            // Call new event
            ModuleBaseEvent newEvent = getEvent();
            Bukkit.getPluginManager().callEvent(newEvent);
            return newEvent;
        }
    }
}