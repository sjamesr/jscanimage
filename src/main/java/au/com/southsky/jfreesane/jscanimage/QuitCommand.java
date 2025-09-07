package au.com.southsky.jfreesane.jscanimage;

import com.beust.jcommander.Parameters;
import java.util.List;

@Parameters(commandNames = "quit", commandDescription = "close the session and quit")
class QuitCommand implements Command {
  @Override
  public void execute(Session session, List<String> parameters) {
    session.setShouldQuit(true);
  }
}
