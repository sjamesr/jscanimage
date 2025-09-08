package au.com.southsky.jfreesane.jscanimage;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import java.util.ArrayList;
import java.util.List;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

@Parameters(commandNames = "set", commandDescription = "set scanner options")
public class SetCommand implements Command {

  @Parameter(description = "the option name and desired value")
  private List<String> optionNames = new ArrayList<>();

  @Override
  public void execute(Session session, List<String> parameters) {}

  @Override
  public Completer getCompleter(Session session) {
    return new ArgumentCompleter(
        new StringsCompleter("set"),
        new ScannerOptionNameCompleter(session),
        new ScannerOptionValueCompleter(session));
  }
}
