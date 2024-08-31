package xyz.geik.gmanager.api.modules.exceptions;

import java.io.Serial;

public abstract class ModuleException extends Exception {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 4203162022348693854L;

    protected ModuleException(String errorMessage){
        super("AddonException : " + errorMessage);
    }

}