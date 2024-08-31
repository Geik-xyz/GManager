package xyz.geik.gmanager.api.modules.exceptions;

import java.io.Serial;

public class InvalidModuleInheritException extends ModuleException {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -5847358994397613244L;

    public InvalidModuleInheritException(String errorMessage) {
        super(errorMessage);
    }

}