package au.com.southsky.jfreesane.jscanimage;

import com.beust.jcommander.Parameters;

import java.util.List;

@Parameters(commandNames = "help", commandDescription = "prints this handy help message")
class HelpCommand implements Command {
  @Override
  public void execute(Session session, List<String> parameters) {
    for (String commandName : session.getMainJCommander().getCommands().keySet()) {
      System.out.println(commandName + "\t");
      session.getMainJCommander().usage(commandName);
    }
  }
}
