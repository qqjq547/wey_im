package framework.telegram.business.commandhandler;

/**
 * Created by hyf on 16/2/25.
 */
public interface CommandHandlers {
    boolean executeCommand(String redirectUrl);

    boolean onBackPressed();
}
