package xyz.geik.gmanager.api.modules.exceptions;

import java.io.Serial;

public class ModuleRequestException extends ModuleException {
    @Serial
    private static final long serialVersionUID = -5698456013070166174L;

    public ModuleRequestException(String errorMessage) {
        super(errorMessage);
    }
}