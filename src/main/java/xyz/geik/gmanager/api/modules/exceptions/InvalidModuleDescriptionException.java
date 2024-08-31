package xyz.geik.gmanager.api.modules.exceptions;

import java.io.Serial;

/**
 * @since 1.11.0
 */
public class InvalidModuleDescriptionException extends ModuleException {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 7741502900847049986L;

    public InvalidModuleDescriptionException(String errorMessage) {
        super(errorMessage);
    }
}