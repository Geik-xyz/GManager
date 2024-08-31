package xyz.geik.gmanager.api.modules.requests;


import org.apache.commons.lang3.Validate;
import xyz.geik.gmanager.GManager;
import xyz.geik.gmanager.modules.Module;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * API to enable plugins to request data from addons.
 * Addons can expose data that they want to expose. To access it, call this class with the appropriate addon name, the label for the
 * data that is requested and if required, a map of key-value pairs that will be given to the addon.
 * @author HyKurtis
 *
 */
public class ModuleRequestBuilder
{
    private String addonName;
    private String requestLabel;
    private final Map<String, Object> metaData = new HashMap<>();

    /**
     * Define the addon you wish to request.
     *
     * @param addonName addon name
     */
    public ModuleRequestBuilder addon(String addonName) {
        this.addonName = addonName;
        return this;
    }

    /**
     * Define label for addon request.
     *
     * @param requestLabel request label
     */
    public ModuleRequestBuilder label(String requestLabel) {
        this.requestLabel = requestLabel;
        return this;
    }

    /**
     * Add meta data to addon request.
     *
     * @param key key
     * @param value value
     */
    public ModuleRequestBuilder addMetaData(String key, Object value) {
        metaData.put(key, value);
        return this;
    }

    /**
     * Send request to addon.
     *
     * @return request response, null if no response.
     */
    public Object request() {
        Validate.notNull(addonName);
        Validate.notNull(requestLabel);

        Optional<Module> addonOptional = GManager.getModuleManager().getAddonByName(addonName);
        if(addonOptional.isPresent()) {
            Module module = addonOptional.get();
            return module.request(requestLabel, metaData);
        }
        return null;
    }
}