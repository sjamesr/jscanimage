package au.com.southsky.jfreesane.jscanimage;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Parameters(commandNames = "help", commandDescription = "prints this handy help message")
class HelpCommand implements Command {

  @Parameter(description = "the name of the device to open (use 'ls' to see a list)")
  private List<String> optionNames = new ArrayList<>();

  @Override
  public void execute(Session session, List<String> parameters) {
    var names = new HashSet<>(optionNames);
    for (String commandName : session.getMainJCommander().getCommands().keySet()) {
      if (names.isEmpty() || names.contains(commandName)) {
        System.out.println(commandName + "\t");
        session.getMainJCommander().usage(commandName);
      }
    }
  }
}
