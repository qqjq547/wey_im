package framework.telegram.support.commandrouter;

import java.lang.reflect.Method;

/**
 * Exception throws when fail to initialize a
 * {@link CommandHandler CommandHandler}.
 *
 * @author Masson
 */
public class CommandInvalidException extends CommandHandlerException {

    public CommandInvalidException(String message, CommandHandler handler, Method method) {
        this(message, null, handler, method);
    }

    public CommandInvalidException(String message, Throwable cause,
                                   CommandHandler handler, Method method) {
        super(message
                + (method == null ? "" : " : " + method.getName())
                + (handler == null ? "" : " at " + handler.getClass().getName()), cause, null);
    }
}
