package framework.telegram.support.commandrouter.driver;


import framework.telegram.support.commandrouter.CommandRouter;

/**
 * Abstract base class for all driver class.
 * Implementations of a driver can parse command arguments
 * from raw command data.
 *
 * @author Masson
 */
public abstract class AbstractDriver {

    public abstract CommandRouter.Op parseCommand(Object context, Object... rawArgs);
}
