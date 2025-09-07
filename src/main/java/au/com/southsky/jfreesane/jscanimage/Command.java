package au.com.southsky.jfreesane.jscanimage;

import org.jline.reader.Completer;
import org.jline.reader.impl.completer.NullCompleter;

import java.util.List;

public interface Command {
  void execute(Session session, List<String> parameters);

  default Completer getCompleter(Session session) {
    return NullCompleter.INSTANCE;
  }
}
