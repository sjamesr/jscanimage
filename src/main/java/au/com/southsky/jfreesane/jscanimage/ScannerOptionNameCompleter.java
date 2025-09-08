package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.SaneOption;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

import java.io.IOException;
import java.util.List;

public class ScannerOptionNameCompleter implements Completer {

  private final Session session;

  public ScannerOptionNameCompleter(Session session) {
    this.session = session;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    if (session.getCurrentDevice() == null) {
      NullCompleter.INSTANCE.complete(reader, line, candidates);
      return;
    }

    try {
      new StringsCompleter(
          session.getCurrentDevice().listOptions().stream()
              .map(SaneOption::getName)
              .filter(name -> !name.isEmpty())
              .toList()).complete(reader, line, candidates);
    } catch (IOException e) {
      // This shouldn't happen: the option list is cached when the device is opened.
      NullCompleter.INSTANCE.complete(reader, line, candidates);
    }
  }
}
